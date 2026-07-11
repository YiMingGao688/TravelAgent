package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class IntentAgent {
    private final AgentStepRecorder recorder;

    public IntentAgent(AgentStepRecorder recorder) {
        this.recorder = recorder;
    }

    public void record(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "IntentAgent", "INTENT_PARSE",
                "需求结构化解析", "正在解析出发地、目的地、时间、场景、预算和偏好", null);
        if (Boolean.TRUE.equals(response.getNeedUserInput())) {
            recorder.waiting(step, "关键信息不足，需要用户补充后继续规划", response.getPartialDemand());
        } else {
            recorder.success(step, "需求解析完成，已进入真实路线和资源候选生成", response.getDemand());
        }
    }
}
