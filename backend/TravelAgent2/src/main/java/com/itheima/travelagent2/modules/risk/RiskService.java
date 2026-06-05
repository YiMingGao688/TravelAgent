package com.itheima.travelagent2.modules.risk;

import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiskService {
    public List<RiskWarningDTO> generateWarnings(StructuredTravelDemand demand, RoutePlanResponseDTO routePlan) {
        List<RiskWarningDTO> warnings = new ArrayList<>();
        String weather = routePlan == null || routePlan.getEndWeather() == null ? null : routePlan.getEndWeather().getWeather();
        if (weather != null && (weather.contains("雨") || weather.contains("雪") || weather.contains("雷"))) {
            warnings.add(warning("MEDIUM", "WEATHER", "目的地天气风险", "目的地天气为" + weather + "，可能影响步行和换乘。", "提前准备雨具并预留 20 分钟缓冲时间。"));
        }
        if (routePlan != null && routePlan.getRoutes() != null && !routePlan.getRoutes().isEmpty()) {
            Route route = routePlan.getRoutes().get(0);
            if (route.getDuration() != null && route.getDuration() > 7200) {
                warnings.add(warning("MEDIUM", "TIME", "行程耗时较长", "推荐路线预计耗时超过 2 小时。", "建议提前出发或选择更快交通方式。"));
            }
            if (route.getDescription() != null && route.getDescription().contains("换乘")) {
                warnings.add(warning("LOW", "ROUTE", "存在换乘节点", "路线包含换乘，行程中需要关注站点和方向。", "开启到站提醒，预留换乘时间。"));
            }
        }
        if (demand != null && demand.getPeopleType() != null && !"GENERAL".equals(demand.getPeopleType())) {
            warnings.add(warning("HIGH", "PEOPLE", "特殊人群服务", "识别到 " + demand.getPeopleType() + " 出行需求。", "优先选择少步行、少换乘路线，必要时预约无障碍服务。"));
        }
        if (demand != null && demand.getLuggageInfo() != null) {
            warnings.add(warning("MEDIUM", "LUGGAGE", "行李携带提醒", "本次出行包含行李信息：" + demand.getLuggageInfo(), "建议选择直达交通、提前确认托运或寄存规则。"));
        }
        if (warnings.isEmpty()) {
            warnings.add(warning("LOW", "ROUTE", "暂无明显风险", "当前需求未触发高风险规则。", "出发前关注天气、订单和站点变化。"));
        }
        return warnings;
    }

    private RiskWarningDTO warning(String level, String type, String title, String content, String suggestion) {
        RiskWarningDTO dto = new RiskWarningDTO();
        dto.setLevel(level);
        dto.setType(type);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setSuggestion(suggestion);
        return dto;
    }
}
