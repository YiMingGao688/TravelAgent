package com.itheima.travelagent2.modules.agenttrip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentEvent;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class AgentEventPublisherService {
    private final AgentEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public AgentEventPublisherService(AgentEventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    public AgentEvent publish(SseEmitter emitter, Long runId, String stage, String title, String summary,
                              String status, Object detail, int sequenceNo) {
        AgentEvent event = new AgentEvent();
        event.setRunId(runId);
        event.setStage(stage);
        event.setTitle(title);
        event.setSummary(summary);
        event.setStatus(status);
        event.setSequenceNo(sequenceNo);
        event.setDetailJson(toJson(detail));
        eventMapper.insert(event);
        send(emitter, "agent_event", event);
        return event;
    }

    public void publishCard(SseEmitter emitter, TripPlanCard card) {
        send(emitter, "trip_card", card);
    }

    public void completed(SseEmitter emitter, Long runId, String message) {
        send(emitter, "completed", java.util.Map.of("runId", String.valueOf(runId), "message", message));
        emitter.complete();
    }

    public void failed(SseEmitter emitter, Long runId, String message) {
        send(emitter, "failed", java.util.Map.of("runId", String.valueOf(runId), "message", message));
        emitter.complete();
    }

    private void send(SseEmitter emitter, String name, Object data) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? java.util.Map.of() : detail);
        } catch (Exception ignored) {
            return "{}";
        }
    }
}
