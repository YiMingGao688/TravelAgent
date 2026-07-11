package com.itheima.travelagent2.modules.agenttrip.agent;

import com.itheima.travelagent2.modules.agenttrip.service.AgentTripStage;
import com.itheima.travelagent2.modules.agenttrip.service.DashScopeAgentModelService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RiskEvaluationAgent {
    private final DashScopeAgentModelService modelService;

    public RiskEvaluationAgent(DashScopeAgentModelService modelService) {
        this.modelService = modelService;
    }

    public Map<String, Object> evaluate(Map<String, Object> input) {
        String instruction = "评估天气、拥堵、时间、换乘、步行距离、证件行李提醒，字段包含 riskLevel,weatherRisk,congestionRisk,timeRisk,transferRisk,walkingRisk,reminders,summary。";
        return modelService.generateJson(AgentTripStage.RISK_EVALUATION, instruction, input)
                .orElseGet(this::fallback);
    }

    private Map<String, Object> fallback() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("riskLevel", "MEDIUM");
        data.put("weatherRisk", "需出行当天确认天气");
        data.put("congestionRisk", "早晚高峰接驳可能拥堵");
        data.put("timeRisk", "建议为进站和换乘预留30分钟");
        data.put("transferRisk", "换乘次数较少，风险可控");
        data.put("walkingRisk", "步行距离中等");
        data.put("reminders", List.of("携带身份证件", "提前查看电子票", "携带充电宝", "关注天气准备雨具"));
        data.put("summary", "风险等级为中，主要风险来自高峰接驳和时间余量。");
        return data;
    }
}
