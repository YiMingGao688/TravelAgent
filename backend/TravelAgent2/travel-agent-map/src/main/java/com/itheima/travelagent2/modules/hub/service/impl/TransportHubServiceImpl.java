package com.itheima.travelagent2.modules.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;
import com.itheima.travelagent2.modules.hub.mapper.TransportHubMapper;
import com.itheima.travelagent2.modules.hub.service.TransportHubService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportHubServiceImpl extends ServiceImpl<TransportHubMapper, TransportHub> implements TransportHubService {

    /**
     * ★ 城市名标准化：把"江苏省南京""南京市""浙江省杭州""杭州市"等统一转成"南京""杭州"。
     * 跨城高铁演示中，南京优先匹配南京南站，杭州优先匹配杭州东站（通过 priority 字段排序）。
     */
    public static String normalizeCityName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String v = raw.trim();
        // 去掉省名前缀
        v = v.replace("江苏省", "").replace("浙江省", "").replace("上海市", "")
             .replace("北京市", "").replace("四川省", "").replace("广东省", "")
             .replace("湖北省", "").replace("湖南省", "").replace("山东省", "")
             .replace("福建省", "").replace("安徽省", "").replace("河南省", "")
             .replace("河北省", "").replace("天津市", "");
        // 去掉"市""省"后缀
        v = v.replace("市", "").replace("省", "");
        // 去掉"特别行政区"等
        return v.trim();
    }

    @Override
    public List<TransportHub> listByCity(String city) {
        LambdaQueryWrapper<TransportHub> wrapper = new LambdaQueryWrapper<>();
        String normalized = normalizeCityName(city);
        if (hasText(normalized)) {
            wrapper.like(TransportHub::getCityName, normalized);
        }
        wrapper.eq(TransportHub::getStatus, "ENABLED")
                .orderByAsc(TransportHub::getPriority)
                .orderByAsc(TransportHub::getId);
        List<TransportHub> result = list(wrapper);
        // 兜底：精确匹配无结果时，用更宽泛的 LIKE
        if (result.isEmpty() && hasText(normalized)) {
            LambdaQueryWrapper<TransportHub> fallback = new LambdaQueryWrapper<>();
            fallback.like(TransportHub::getCityName, normalized.substring(0, Math.min(2, normalized.length())));
            fallback.eq(TransportHub::getStatus, "ENABLED")
                    .orderByAsc(TransportHub::getPriority)
                    .orderByAsc(TransportHub::getId);
            List<TransportHub> fb = list(fallback);
            if (!fb.isEmpty()) {
                System.out.println("[HUB] listByCity fallback: raw=" + city + " normalized=" + normalized + " count=" + fb.size());
                return fb;
            }
        }
        if (!result.isEmpty()) {
            System.out.println("[HUB] listByCity raw=" + city + " normalized=" + normalized + " count=" + result.size());
        }
        return result;
    }

    @Override
    public TransportHub recommend(String startCity, String endCity, String mode, boolean startSide) {
        String city = startSide ? startCity : endCity;
        String normalized = normalizeCityName(city);
        String hubType = switch ((mode == null ? "TRAIN" : mode).toUpperCase()) {
            case "FLIGHT" -> "AIRPORT";
            case "BUS" -> "BUS_STATION";
            case "SUBWAY" -> "SUBWAY_STATION";
            default -> "TRAIN_STATION";
        };
        LambdaQueryWrapper<TransportHub> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(TransportHub::getCityName, normalized)
                .eq(TransportHub::getHubType, hubType)
                .eq(TransportHub::getStatus, "ENABLED")
                .orderByAsc(TransportHub::getPriority)
                .orderByAsc(TransportHub::getId)
                .last("LIMIT 1");
        TransportHub hub = getOne(wrapper, false);

        // 兜底：精确匹配无结果时用更短的关键词
        if (hub == null && normalized.length() >= 2) {
            LambdaQueryWrapper<TransportHub> fallback = new LambdaQueryWrapper<>();
            fallback.like(TransportHub::getCityName, normalized.substring(0, 2))
                    .eq(TransportHub::getHubType, hubType)
                    .eq(TransportHub::getStatus, "ENABLED")
                    .orderByAsc(TransportHub::getPriority)
                    .orderByAsc(TransportHub::getId)
                    .last("LIMIT 1");
            hub = getOne(fallback, false);
            if (hub != null) {
                System.out.println("[HUB] recommend fallback: raw=" + city + " normalized=" + normalized + " hub=" + hub.getHubName());
            }
        }

        if (hub != null) {
            System.out.println("[HUB] recommend city=" + normalized + " type=" + hubType + " result=" + hub.getHubName());
        } else {
            System.out.println("[HUB] recommend FAIL city=" + normalized + " type=" + hubType + " raw=" + city);
        }
        return hub;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
