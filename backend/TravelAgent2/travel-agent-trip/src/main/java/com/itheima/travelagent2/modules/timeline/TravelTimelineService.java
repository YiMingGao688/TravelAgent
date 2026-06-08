package com.itheima.travelagent2.modules.timeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import com.itheima.travelagent2.modules.timeline.mapper.TravelTimelineItemMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TravelTimelineService {
    private final TravelTimelineItemMapper mapper;

    public TravelTimelineService(TravelTimelineItemMapper mapper) {
        this.mapper = mapper;
    }

    public List<TravelTimelineItem> generate(String userId, Long tripPlanId, StructuredTravelDemand demand) {
        List<TravelTimelineItem> items = new ArrayList<>();
        add(items, userId, tripPlanId, "BEFORE", "WEATHER", "天气提醒", "出发前查看起终点天气并准备雨具。", "出发前 24 小时");
        add(items, userId, tripPlanId, "BEFORE", "ID_CARD", "证件提醒", "检查身份证、订单凭证和联系人电话。", "出发前 12 小时");
        add(items, userId, tripPlanId, "BEFORE", "LUGGAGE", "行李提醒", demand != null && demand.getLuggageInfo() != null ? "确认行李托运、寄存或随身携带规则。" : "按出行天数检查随身物品。", "出发前 6 小时");
        add(items, userId, tripPlanId, "BEFORE", "DEPARTURE", "出发提醒", "建议提前到达车站/机场，预留安检和换乘时间。", "出发前 2 小时");
        add(items, userId, tripPlanId, "DURING", "ROUTE", "路线提醒", "按规划路线执行，异常时可重新规划。", "行程开始");
        add(items, userId, tripPlanId, "DURING", "TRANSFER", "换乘提醒", "换乘前确认站台、方向和末班时间。", "换乘前 15 分钟");
        add(items, userId, tripPlanId, "DURING", "CONGESTION", "拥堵提醒", "遇到拥堵时自动建议替代交通。", "行程中");
        add(items, userId, tripPlanId, "DURING", "ARRIVAL", "到站提醒", "到达后检查随身行李并前往酒店/目的地。", "到站前 10 分钟");
        add(items, userId, tripPlanId, "AFTER", "AFTER_SALE", "售后入口", "支持改签、退订、投诉、行李、延误、订单异常处理。", "行后");
        add(items, userId, tripPlanId, "AFTER", "FEEDBACK", "评价反馈", "收集体验评价用于后续个性化推荐。", "行后");
        add(items, userId, tripPlanId, "AFTER", "REPORT", "报告生成", "生成全过程服务报告。", "行后");
        add(items, userId, tripPlanId, "AFTER", "REUSE", "历史复用", "本次方案可作为下次同类出行模板。", "行后");
        for (TravelTimelineItem item : items) mapper.insert(item);
        return items;
    }

    public List<TravelTimelineItem> listByUserId(String userId) {
        return mapper.selectList(new LambdaQueryWrapper<TravelTimelineItem>()
                .eq(TravelTimelineItem::getUserId, userId)
                .orderByDesc(TravelTimelineItem::getCreateTime));
    }

    public List<TravelTimelineItem> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<TravelTimelineItem>().orderByDesc(TravelTimelineItem::getCreateTime));
    }

    private void add(List<TravelTimelineItem> items, String userId, Long tripPlanId, String stage, String serviceType, String title, String content, String triggerTime) {
        TravelTimelineItem item = new TravelTimelineItem();
        item.setUserId(userId);
        item.setTripPlanId(tripPlanId);
        item.setStage(stage);
        item.setServiceType(serviceType);
        item.setTitle(title);
        item.setContent(content);
        item.setTriggerTime(triggerTime);
        item.setStatus("PENDING");
        items.add(item);
    }
}
