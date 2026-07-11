package com.itheima.travelagent2.modules.agenttrip.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TripCardGenerationAgent {
    private final ObjectMapper objectMapper;

    public TripCardGenerationAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public List<TripPlanCard> generate(Long runId, Map<String, Object> intent, Map<String, Object> route,
                                       Map<String, Object> resource, Map<String, Object> risk) {
        List<TripPlanCard> cards = new ArrayList<>();
        cards.add(card(runId, "PREPARE", "行前准备", "证件与行李提醒",
                "携带身份证件、充电宝，提前确认电子票和天气变化。", risk, false, 10));

        List<TrafficProduct> traffic = (List<TrafficProduct>) resource.getOrDefault("trafficCandidates", List.of());
        if (!traffic.isEmpty()) {
            TrafficProduct first = traffic.get(0);
            cards.add(card(runId, transportType(first.getType()), first.getStartPlace() + " → " + first.getEndPlace(),
                    first.getCarrierName() + " · " + first.getSeatType(),
                    "预计" + first.getDuration() + "，参考费用￥" + first.getPrice() + "，余票" + first.getRemainCount() + "张。",
                    first, true, 20));
        } else {
            cards.add(card(runId, "TRANSPORT", stringValue(intent.get("startPlace")) + " → " + stringValue(intent.get("endPlace")),
                    "本地模拟库暂无票务匹配", "当前模拟数据库没有匹配票务，可换方案或补充更精确时间。", resource, true, 20));
        }

        cards.add(card(runId, "TRANSFER", "到站接驳", "打车/地铁接驳候选",
                "建议到站后根据行李和天气选择打车或地铁，预计接驳约25分钟。", resource.get("taxiCandidate"), true, 30));

        List<Hotel> hotels = (List<Hotel>) resource.getOrDefault("hotelCandidates", List.of());
        if (!hotels.isEmpty()) {
            Hotel hotel = hotels.get(0);
            cards.add(card(runId, "HOTEL", hotel.getName(), hotel.getAddress(),
                    "评分" + hotel.getScore() + "，参考价￥" + hotel.getMinPrice() + "起，等待用户确认后加入行程单。",
                    hotel, true, 40));
        } else {
            cards.add(card(runId, "HOTEL", stringValue(intent.get("endPlace")) + "住宿候选",
                    "本地模拟库暂无酒店匹配", "当前模拟数据库没有匹配酒店，可调整地点或预算后重新查询。", resource, true, 40));
        }

        cards.add(card(runId, "REMINDER", "到达后提醒", "风险与时间提醒",
                "到达后确认返程时间、发票需求和第二天出行安排。", risk, false, 50));
        return cards;
    }

    private TripPlanCard card(Long runId, String type, String title, String subtitle, String content,
                              Object detail, boolean confirmRequired, int sequenceNo) {
        TripPlanCard card = new TripPlanCard();
        card.setRunId(runId);
        card.setCardType(type);
        card.setTitle(title);
        card.setSubtitle(subtitle);
        card.setContent(content);
        card.setConfirmRequired(confirmRequired);
        card.setConfirmed(!confirmRequired);
        card.setStatus(confirmRequired ? "PENDING" : "AUTO_INCLUDED");
        card.setSequenceNo(sequenceNo);
        try {
            card.setDetailJson(objectMapper.writeValueAsString(detail));
        } catch (Exception ignored) {
            card.setDetailJson("{}");
        }
        return card;
    }

    private String transportType(String type) {
        String value = String.valueOf(type == null ? "" : type).toUpperCase();
        if (value.contains("FLIGHT")) return "FLIGHT";
        if (value.contains("TRAIN")) return "TRAIN";
        return "TRANSPORT";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
