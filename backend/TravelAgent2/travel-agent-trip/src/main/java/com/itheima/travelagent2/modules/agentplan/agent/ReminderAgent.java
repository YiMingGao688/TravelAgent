package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.mapper.TripCandidateMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReminderAgent {
    private final AgentStepRecorder recorder;
    private final TripCandidateMapper candidateMapper;

    public ReminderAgent(AgentStepRecorder recorder, TripCandidateMapper candidateMapper) {
        this.recorder = recorder;
        this.candidateMapper = candidateMapper;
    }

    public int generate(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "ReminderAgent", "REMINDER_CANDIDATE",
                "提醒事项候选生成", "正在生成证件、雨具、充电宝、电子票等提醒事项", response.getReminderTemplates());
        List<ReminderTemplate> reminders = response.getReminderTemplates() == null ? List.of() : response.getReminderTemplates();
        int count = 0;
        for (int i = 0; i < reminders.size() && i < 6; i++) {
            ReminderTemplate reminder = reminders.get(i);
            TripCandidate candidate = new TripCandidate();
            candidate.setSessionId(sessionId);
            candidate.setCandidateType("REMINDER");
            candidate.setTitle(reminder.getTitle());
            candidate.setProvider("REMINDER_TEMPLATE");
            candidate.setPrice(BigDecimal.ZERO);
            candidate.setScore(BigDecimal.valueOf(85));
            candidate.setReason(firstText(reminder.getContentTemplate(), "行程执行提醒，需用户确认后加入行程单。"));
            candidate.setDetailJson(recorder.toJson(reminder));
            candidate.setStatus("PENDING");
            candidateMapper.insert(candidate);
            count++;
        }
        recorder.success(step, "已生成 " + count + " 个提醒候选，等待用户确认", java.util.Map.of("count", count));
        return count;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
