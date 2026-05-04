package com.itheima.travelagent2.modules.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.trip.dto.ItineraryGenerateDTO;
import com.itheima.travelagent2.modules.trip.dto.TripPlanSaveDTO;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    private final ZhipuAiClient zhipuAiClient;
    private final TripPlanService tripPlanService;
    private final ObjectMapper objectMapper;

    public ItineraryController(ZhipuAiClient zhipuAiClient, TripPlanService tripPlanService, ObjectMapper objectMapper) {
        this.zhipuAiClient = zhipuAiClient;
        this.tripPlanService = tripPlanService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/generate")
    public Result<TripPlan> generate(@RequestBody ItineraryGenerateDTO dto) {
        if (blank(dto.getUserId())) {
            return Result.error(400, "userId 不能为空");
        }
        if (blank(dto.getDestination())) {
            return Result.error(400, "目的地不能为空");
        }
        int days = dto.getDays() == null || dto.getDays() < 1 ? 1 : Math.min(dto.getDays(), 15);
        String systemPrompt = """
                你是面向旅客的个性化行程规划助手。
                只输出 JSON，不要输出 Markdown，不要解释。
                JSON 字段：title, summary, suitableFor, riskReminder, trafficAdvice, foodAdvice, days。
                days 是数组，每天包含 day, theme, morning, afternoon, evening, traffic, food, risk。
                所有内容必须是简体中文，建议具体、可执行，不要编造实时票价和实时库存。
                """;
        String userPrompt = """
                出发地：%s
                目的地：%s
                出行天数：%s
                出行日期：%s
                人群类型：%s
                出行偏好：%s
                预算：%s
                请生成卡片化行程 JSON。
                """.formatted(
                value(dto.getStartPlace()),
                value(dto.getDestination()),
                days,
                value(dto.getTravelDate()),
                value(dto.getCrowdType()),
                value(dto.getPreference()),
                value(dto.getBudget())
        );
        String aiJson = zhipuAiClient.chat(systemPrompt, userPrompt);
        String normalized = normalizeJson(aiJson);

        TripPlanSaveDTO saveDTO = new TripPlanSaveDTO();
        saveDTO.setUserId(dto.getUserId());
        saveDTO.setSessionId(dto.getSessionId());
        saveDTO.setTitle(titleFromJson(normalized, dto.getDestination(), days));
        saveDTO.setStartPlace(dto.getStartPlace());
        saveDTO.setEndPlace(dto.getDestination());
        saveDTO.setContent(normalized);
        saveDTO.setRawQuestion(toJson(dto));
        saveDTO.setSourceType("ITINERARY");
        saveDTO.setTravelPreference(dto.getPreference());
        saveDTO.setBudget(dto.getBudget());
        saveDTO.setDays(days);
        return Result.success("行程已生成并保存", tripPlanService.savePlan(saveDTO));
    }

    private String normalizeJson(String value) {
        if (value == null) {
            return "{}";
        }
        String text = value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = text.lastIndexOf("```");
            if (end >= 0) {
                text = text.substring(0, end);
            }
        }
        return text.trim();
    }

    @SuppressWarnings("unchecked")
    private String titleFromJson(String json, String destination, int days) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object title = map.get("title");
            if (title != null && !String.valueOf(title).isBlank()) {
                return String.valueOf(title);
            }
        } catch (Exception ignored) {
        }
        return destination + days + "日个性化行程";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String value(String value) {
        return blank(value) ? "未提供" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
