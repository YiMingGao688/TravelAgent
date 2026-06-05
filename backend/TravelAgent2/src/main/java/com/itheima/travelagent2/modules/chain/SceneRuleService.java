package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SceneRuleService {
    public void apply(StructuredTravelDemand demand) {
        if (demand == null) {
            return;
        }
        if (isBusinessTrip(demand)) {
            demand.setSceneType("BUSINESS_TRIP");
            if (demand.getSortRule() == null || demand.getSortRule().isBlank()) {
                demand.setSortRule("SMART");
            }
            List<String> reminders = new ArrayList<>(demand.getReminderItems() == null ? List.of() : demand.getReminderItems());
            reminders.addAll(List.of("身份证", "电脑", "充电器", "会议文件", "发票抬头", "工牌/门禁卡"));
            demand.setReminderItems(reminders.stream().distinct().toList());
        }
    }

    private boolean isBusinessTrip(StructuredTravelDemand demand) {
        if ("BUSINESS_TRIP".equals(demand.getSceneType())) {
            return true;
        }
        String end = demand.getEndPlace() == null ? "" : demand.getEndPlace();
        return end.contains("公司") || end.contains("总部") || end.contains("园区");
    }
}
