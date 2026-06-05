package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.chain.dto.TravelChainRequestDTO;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClarifyService {
    public List<String> checkMissingFields(StructuredTravelDemand demand, TravelChainRequestDTO request) {
        List<String> missing = new ArrayList<>();
        if (!hasText(demand.getStartPlace()) || !hasLngLat(request.getStartLocation())) {
            missing.add("startPlace/startGeo");
        }
        if (!hasText(demand.getEndPlace()) || !hasLngLat(request.getEndLocation())) {
            missing.add("endPlace/endGeo");
        }
        if (!hasText(demand.getDepartureDate())) {
            missing.add("departureDate");
        }
        if (!hasText(demand.getDepartureTime())) {
            missing.add("departureTime");
        }
        if (demand.getDays() != null && demand.getDays() >= 2 && demand.getNeedHotel() == null) {
            missing.add("needHotel");
        }
        if ("BUSINESS_TRIP".equals(demand.getSceneType())
                && (demand.getReminderItems() == null || demand.getReminderItems().isEmpty())) {
            missing.add("reminderItems");
        }
        if (demand.getAmbiguousPlaces() != null && !demand.getAmbiguousPlaces().isEmpty()) {
            missing.add("placeConfirmation");
        }
        return missing.stream().distinct().toList();
    }

    public String buildQuestion(List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return null;
        }
        List<String> text = new ArrayList<>();
        if (missing.contains("departureDate")) text.add("出发日期");
        if (missing.contains("departureTime")) text.add("预计出发时间");
        if (missing.contains("needHotel")) text.add("出差两天是否需要我匹配目的地附近酒店");
        if (missing.contains("reminderItems")) text.add("证件和文件提醒清单");
        if (missing.contains("placeConfirmation")) text.add("具体目的地点候选");
        if (missing.contains("startPlace/startGeo")) text.add("已确认坐标的起始地");
        if (missing.contains("endPlace/endGeo")) text.add("已确认坐标的终点地");
        return "请补充或确认：" + String.join("、", text) + "，我再继续生成完整出行方案。";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasLngLat(String value) {
        if (!hasText(value) || !value.contains(",")) return false;
        String[] parts = value.split(",");
        if (parts.length != 2) return false;
        try {
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
