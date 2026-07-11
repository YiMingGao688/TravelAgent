package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanStartRequestDTO;
import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.PlanSession;
import com.itheima.travelagent2.modules.agentplan.mapper.PlanSessionMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.TravelChainService;
import com.itheima.travelagent2.modules.chain.dto.TravelChainRequestDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TravelMasterAgent {
    private final PlanSessionMapper sessionMapper;
    private final AgentStepRecorder recorder;
    private final TravelChainService travelChainService;
    private final IntentAgent intentAgent;
    private final RouteAgent routeAgent;
    private final TicketAgent ticketAgent;
    private final HotelAgent hotelAgent;
    private final RiskAgent riskAgent;
    private final ReminderAgent reminderAgent;

    public TravelMasterAgent(PlanSessionMapper sessionMapper,
                             AgentStepRecorder recorder,
                             TravelChainService travelChainService,
                             IntentAgent intentAgent,
                             RouteAgent routeAgent,
                             TicketAgent ticketAgent,
                             HotelAgent hotelAgent,
                             RiskAgent riskAgent,
                             ReminderAgent reminderAgent) {
        this.sessionMapper = sessionMapper;
        this.recorder = recorder;
        this.travelChainService = travelChainService;
        this.intentAgent = intentAgent;
        this.routeAgent = routeAgent;
        this.ticketAgent = ticketAgent;
        this.hotelAgent = hotelAgent;
        this.riskAgent = riskAgent;
        this.reminderAgent = reminderAgent;
    }

    public PlanSession start(AgentPlanStartRequestDTO request) {
        PlanSession session = new PlanSession();
        session.setUserId(firstText(request.getUserId(), "test_user"));
        session.setSessionTitle(firstText(request.getSessionTitle(), request.getStartPlace() + " 到 " + request.getEndPlace() + " 智能行程"));
        session.setStartPlace(request.getStartPlace());
        session.setEndPlace(request.getEndPlace());
        session.setDepartTime(request.getDepartTime());
        session.setReturnTime(request.getReturnTime());
        session.setSceneType(request.getSceneType());
        session.setPreference(request.getPreference());
        session.setBudget(request.getBudget());
        session.setStatus("RUNNING");
        session.setCurrentStep("MASTER_START");
        sessionMapper.insert(session);

        AgentStepRecord masterStep = recorder.start(session.getId(), "TravelMasterAgent", "MASTER_START",
                "智能体工作流启动", "正在启动真实智能体规划流程", request);
        if (!hasLngLat(request.getStartLocation()) || !hasLngLat(request.getEndLocation())) {
            session.setStatus("WAIT_USER_CONFIRM");
            session.setCurrentStep("GEO_CONFIRM");
            sessionMapper.updateById(session);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("startLocation", request.getStartLocation());
            output.put("endLocation", request.getEndLocation());
            recorder.waiting(masterStep, "起始地和目的地必须先从高德候选中确认经纬度，暂不生成路线/票务/酒店候选",
                    output);
            return session;
        }

        try {
            TravelChainResponseDTO response = travelChainService.plan(toChainRequest(session, request));
            intentAgent.record(session.getId(), response);
            if (Boolean.TRUE.equals(response.getNeedUserInput())) {
                session.setStatus("WAIT_USER_INPUT");
                session.setCurrentStep("INTENT_PARSE");
                sessionMapper.updateById(session);
                recorder.waiting(masterStep, "智能体需要用户补充信息后继续规划", response);
                return session;
            }
            routeAgent.generate(session.getId(), response);
            ticketAgent.generate(session.getId(), response);
            hotelAgent.generate(session.getId(), response);
            riskAgent.record(session.getId(), response);
            reminderAgent.generate(session.getId(), response);
            session.setStatus("WAIT_USER_CONFIRM");
            session.setCurrentStep("USER_CONFIRM_CANDIDATE");
            sessionMapper.updateById(session);
            recorder.success(masterStep, "真实智能体工作流已生成候选项，等待用户确认加入行程单", response.getSteps());
            return session;
        } catch (Exception exception) {
            session.setStatus("FAILED");
            session.setCurrentStep("FAILED");
            sessionMapper.updateById(session);
            recorder.fail(masterStep, "智能体规划失败：" + exception.getMessage(), exception);
            throw exception;
        }
    }

    public void regenerateCandidates(Long sessionId, AgentPlanStartRequestDTO request, String candidateType) {
        AgentStepRecord step = recorder.start(sessionId, "TravelMasterAgent", "CHANGE_CANDIDATE",
                "候选换方案", "正在根据用户要求重新生成候选", Map.of("candidateType", candidateType));
        TravelChainResponseDTO response = travelChainService.plan(toChainRequest(sessionId, request));
        switch (candidateType) {
            case "TRAIN", "FLIGHT" -> ticketAgent.generate(sessionId, response);
            case "HOTEL" -> hotelAgent.generate(sessionId, response);
            case "REMINDER" -> reminderAgent.generate(sessionId, response);
            default -> routeAgent.generate(sessionId, response);
        }
        recorder.success(step, "已重新生成同类型候选，请用户重新选择", Map.of("candidateType", candidateType));
    }

    private TravelChainRequestDTO toChainRequest(PlanSession session, AgentPlanStartRequestDTO request) {
        return toChainRequest(session.getId(), request);
    }

    private TravelChainRequestDTO toChainRequest(Long sessionId, AgentPlanStartRequestDTO request) {
        TravelChainRequestDTO dto = new TravelChainRequestDTO();
        dto.setUserId(firstText(request.getUserId(), "test_user"));
        dto.setSessionId(String.valueOf(sessionId));
        dto.setStartPlace(request.getStartPlace());
        dto.setEndPlace(request.getEndPlace());
        dto.setStartLocation(request.getStartLocation());
        dto.setEndLocation(request.getEndLocation());
        dto.setStartAdcode(request.getStartAdcode());
        dto.setEndAdcode(request.getEndAdcode());
        dto.setStartCity(request.getStartCity());
        dto.setEndCity(request.getEndCity());
        dto.setMessage(firstText(request.getMessage(), request.getPreference(), request.getStartPlace() + " 到 " + request.getEndPlace()));
        return dto;
    }

    private boolean hasLngLat(String value) {
        if (value == null || value.isBlank() || !value.contains(",")) {
            return false;
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return false;
        }
        try {
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            return true;
        } catch (Exception ignored) {
            return false;
        }
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
