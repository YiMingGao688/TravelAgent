package com.itheima.travelagent2.modules.agenttrip.agent;

import com.itheima.travelagent2.modules.agenttrip.dto.TripPlanStreamRequestDTO;
import com.itheima.travelagent2.modules.agenttrip.service.AgentTripStage;
import com.itheima.travelagent2.modules.agenttrip.service.DashScopeAgentModelService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequirementAnalysisAgent {
    private final DashScopeAgentModelService modelService;

    public RequirementAnalysisAgent(DashScopeAgentModelService modelService) {
        this.modelService = modelService;
    }

    public Map<String, Object> analyze(TripPlanStreamRequestDTO request) {
        String instruction = """
                解析出行需求，JSON 字段必须包含 startPlace,endPlace,days,date,preference,budget,peopleCount,sceneType,missingFields,summary。
                地点字段只放地点名，不要把“出差两天”“玩两天”等需求文字识别成地点。
                """;
        return modelService.generateJson(AgentTripStage.REQUIREMENT_ANALYSIS, instruction, request)
                .map(result -> normalize(request, result))
                .orElseGet(() -> fallback(request));
    }

    private Map<String, Object> normalize(TripPlanStreamRequestDTO request, Map<String, Object> result) {
        Map<String, Object> data = fallback(request);
        data.putAll(result);
        data.put("startPlace", firstText(value(data.get("startPlace")), request.getStartPlace()));
        data.put("endPlace", firstText(value(data.get("endPlace")), request.getEndPlace()));
        return data;
    }

    private Map<String, Object> fallback(TripPlanStreamRequestDTO request) {
        List<String> missing = new ArrayList<>();
        if (isBlank(request.getDepartDate())) missing.add("出发日期");
        if (request.getBudget() == null) missing.add("预算");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startPlace", firstText(request.getStartPlace(), "起点待确认"));
        data.put("endPlace", firstText(request.getEndPlace(), "终点待确认"));
        data.put("days", inferDays(request.getRequirementText()));
        data.put("date", request.getDepartDate());
        data.put("preference", firstText(request.getPreference(), "效率优先"));
        data.put("budget", request.getBudget());
        data.put("peopleCount", request.getPeopleCount() == null ? 1 : request.getPeopleCount());
        data.put("sceneType", firstText(request.getSceneType(), inferScene(request.getRequirementText())));
        data.put("missingFields", missing);
        data.put("summary", "已识别出发地、目的地、出行天数和偏好，缺失信息将等待用户补充。");
        return data;
    }

    private Integer inferDays(String text) {
        String value = String.valueOf(text == null ? "" : text);
        if (value.contains("三天") || value.contains("3天")) return 3;
        if (value.contains("两天") || value.contains("2天")) return 2;
        return 1;
    }

    private String inferScene(String text) {
        String value = String.valueOf(text == null ? "" : text);
        if (value.contains("出差") || value.contains("商务")) return "商务出行";
        if (value.contains("亲子")) return "亲子出行";
        return "综合出行";
    }

    private String firstText(String... values) {
        for (String item : values) {
            if (!isBlank(item)) return item;
        }
        return "";
    }

    private String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
