package com.itheima.travelagent2.modules.agentplan.service;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.mapper.AgentStepRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AgentStepRecorder {
    private final AgentStepRecordMapper stepMapper;
    private final ObjectMapper objectMapper;

    public AgentStepRecorder(AgentStepRecordMapper stepMapper, ObjectMapper objectMapper) {
        this.stepMapper = stepMapper;
        this.objectMapper = objectMapper;
    }

    public AgentStepRecord start(Long sessionId, String agentName, String stepKey, String title, String content, Object input) {
        AgentStepRecord step = new AgentStepRecord();
        step.setSessionId(sessionId);
        step.setAgentName(agentName);
        step.setStepKey(stepKey);
        step.setStepTitle(title);
        step.setStepContent(content);
        step.setStatus("RUNNING");
        step.setInputJson(toJson(input));
        step.setStartedAt(LocalDateTime.now());
        stepMapper.insert(step);
        return step;
    }

    public void success(AgentStepRecord step, String content, Object output) {
        step.setStatus("SUCCESS");
        step.setStepContent(content);
        step.setOutputJson(toJson(output));
        step.setFinishedAt(LocalDateTime.now());
        stepMapper.updateById(step);
    }

    public void waiting(AgentStepRecord step, String content, Object output) {
        step.setStatus("WAITING_CONFIRM");
        step.setStepContent(content);
        step.setOutputJson(toJson(output));
        step.setFinishedAt(LocalDateTime.now());
        stepMapper.updateById(step);
    }

    public void fail(AgentStepRecord step, String content, Exception exception) {
        step.setStatus("FAILED");
        step.setStepContent(content);
        step.setOutputJson(toJson(java.util.Map.of("error", exception.getMessage())));
        step.setFinishedAt(LocalDateTime.now());
        stepMapper.updateById(step);
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }
}
