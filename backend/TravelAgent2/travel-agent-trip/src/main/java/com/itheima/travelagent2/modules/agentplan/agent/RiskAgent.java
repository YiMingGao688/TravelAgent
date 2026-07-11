package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class RiskAgent {
    private final AgentStepRecorder recorder;

    public RiskAgent(AgentStepRecorder recorder) {
        this.recorder = recorder;
    }

    public void record(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "RiskAgent", "RISK_ANALYSIS",
                "风险分析", "正在结合天气、路线、换乘时间生成风险提醒", response.getWeatherInfo());
        int count = response.getRisks() == null ? 0 : response.getRisks().size();
        recorder.success(step, "已生成 " + count + " 条风险提醒，将在最终总行程卡片中汇总", response.getRisks());
    }
}
