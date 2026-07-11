package com.itheima.travelagent2.modules.agenttrip.agent;

import com.itheima.travelagent2.modules.agenttrip.service.AgentTripStage;
import com.itheima.travelagent2.modules.agenttrip.service.DashScopeAgentModelService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RouteCandidateAgent {
    private final DashScopeAgentModelService modelService;

    public RouteCandidateAgent(DashScopeAgentModelService modelService) {
        this.modelService = modelService;
    }

    public Map<String, Object> generate(Map<String, Object> intent) {
        String instruction = "生成路线候选，字段包含 recommendedTraffic,connection,estimatedDuration,estimatedCost,transferCount,walkingDistance,summary。";
        return modelService.generateJson(AgentTripStage.ROUTE_CANDIDATE_GENERATION, instruction, intent)
                .orElseGet(() -> fallback(intent));
    }

    private Map<String, Object> fallback(Map<String, Object> intent) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recommendedTraffic", "高铁优先");
        data.put("connection", "打车或地铁接驳");
        data.put("estimatedDuration", "约2小时");
        data.put("estimatedCost", "约260元");
        data.put("transferCount", 1);
        data.put("walkingDistance", "约900米");
        data.put("summary", "已生成高铁优先路线，并预留到站接驳方案。");
        data.put("startPlace", intent.get("startPlace"));
        data.put("endPlace", intent.get("endPlace"));
        return data;
    }
}
