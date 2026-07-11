package com.itheima.travelagent2.modules.agenttrip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.agenttrip.agent.RequirementAnalysisAgent;
import com.itheima.travelagent2.modules.agenttrip.agent.RiskEvaluationAgent;
import com.itheima.travelagent2.modules.agenttrip.agent.RouteCandidateAgent;
import com.itheima.travelagent2.modules.agenttrip.agent.TicketHotelMatchingAgent;
import com.itheima.travelagent2.modules.agenttrip.agent.TripCardGenerationAgent;
import com.itheima.travelagent2.modules.agenttrip.dto.AgentTripRunDetailDTO;
import com.itheima.travelagent2.modules.agenttrip.dto.TripPlanStreamRequestDTO;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentEvent;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentRun;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentEventMapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentRunMapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.TripPlanCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TravelPlanningAgentService {
    private final AgentRunMapper runMapper;
    private final AgentEventMapper eventMapper;
    private final TripPlanCardMapper cardMapper;
    private final RequirementAnalysisAgent requirementAnalysisAgent;
    private final RouteCandidateAgent routeCandidateAgent;
    private final TicketHotelMatchingAgent ticketHotelMatchingAgent;
    private final RiskEvaluationAgent riskEvaluationAgent;
    private final TripCardGenerationAgent tripCardGenerationAgent;
    private final AgentEventPublisherService eventPublisher;
    private final TripCardConfirmService confirmService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TravelPlanningAgentService(AgentRunMapper runMapper,
                                      AgentEventMapper eventMapper,
                                      TripPlanCardMapper cardMapper,
                                      RequirementAnalysisAgent requirementAnalysisAgent,
                                      RouteCandidateAgent routeCandidateAgent,
                                      TicketHotelMatchingAgent ticketHotelMatchingAgent,
                                      RiskEvaluationAgent riskEvaluationAgent,
                                      TripCardGenerationAgent tripCardGenerationAgent,
                                      AgentEventPublisherService eventPublisher,
                                      TripCardConfirmService confirmService) {
        this.runMapper = runMapper;
        this.eventMapper = eventMapper;
        this.cardMapper = cardMapper;
        this.requirementAnalysisAgent = requirementAnalysisAgent;
        this.routeCandidateAgent = routeCandidateAgent;
        this.ticketHotelMatchingAgent = ticketHotelMatchingAgent;
        this.riskEvaluationAgent = riskEvaluationAgent;
        this.tripCardGenerationAgent = tripCardGenerationAgent;
        this.eventPublisher = eventPublisher;
        this.confirmService = confirmService;
    }

    public SseEmitter streamPlan(TripPlanStreamRequestDTO request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        AgentRun run = createRun(request);
        executor.execute(() -> executeWorkflow(emitter, run, request));
        return emitter;
    }

    public AgentTripRunDetailDTO detail(Long runId) {
        AgentTripRunDetailDTO detail = new AgentTripRunDetailDTO();
        detail.setRun(runMapper.selectById(runId));
        detail.setEvents(events(runId));
        detail.setCards(cards(runId));
        detail.setFinalItinerary(confirmService.listFinal(runId));
        return detail;
    }

    public List<AgentEvent> events(Long runId) {
        return eventMapper.selectList(new LambdaQueryWrapper<AgentEvent>()
                .eq(AgentEvent::getRunId, runId)
                .orderByAsc(AgentEvent::getSequenceNo)
                .orderByAsc(AgentEvent::getId));
    }

    public List<TripPlanCard> cards(Long runId) {
        return cardMapper.selectList(new LambdaQueryWrapper<TripPlanCard>()
                .eq(TripPlanCard::getRunId, runId)
                .orderByAsc(TripPlanCard::getSequenceNo)
                .orderByAsc(TripPlanCard::getId));
    }

    private AgentRun createRun(TripPlanStreamRequestDTO request) {
        AgentRun run = new AgentRun();
        run.setUserId(firstText(request.getUserId(), "test_user"));
        run.setRequestId(firstText(request.getRequestId(), UUID.randomUUID().toString()));
        run.setStartPlace(request.getStartPlace());
        run.setEndPlace(request.getEndPlace());
        run.setRequirementText(request.getRequirementText());
        run.setStatus("RUNNING");
        run.setCurrentStage(AgentTripStage.REQUIREMENT_ANALYSIS);
        runMapper.insert(run);
        return run;
    }

    private void executeWorkflow(SseEmitter emitter, AgentRun run, TripPlanStreamRequestDTO request) {
        int sequence = 1;
        try {
            publishRunState(run, AgentTripStage.REQUIREMENT_ANALYSIS);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.REQUIREMENT_ANALYSIS, "解析用户需求",
                    "正在解析起点、终点、日期、预算、人数和偏好。", "RUNNING", request, sequence++);
            Map<String, Object> intent = requirementAnalysisAgent.analyze(request);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.REQUIREMENT_ANALYSIS, "解析用户需求",
                    stringValue(intent.get("summary")), "DONE", intent, sequence++);

            publishRunState(run, AgentTripStage.ROUTE_CANDIDATE_GENERATION);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.ROUTE_CANDIDATE_GENERATION, "生成路线候选",
                    "正在生成交通方式、接驳、耗时、费用和步行距离候选。", "RUNNING", intent, sequence++);
            Map<String, Object> route = routeCandidateAgent.generate(intent);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.ROUTE_CANDIDATE_GENERATION, "生成路线候选",
                    stringValue(route.get("summary")), "DONE", route, sequence++);

            publishRunState(run, AgentTripStage.TICKET_HOTEL_MATCHING);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.TICKET_HOTEL_MATCHING, "匹配票务和住宿候选",
                    "正在从本地模拟数据库搜索高铁、机票、接驳和酒店候选。", "RUNNING", route, sequence++);
            Map<String, Object> resource = ticketHotelMatchingAgent.match(intent);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.TICKET_HOTEL_MATCHING, "匹配票务和住宿候选",
                    stringValue(resource.get("summary")), "DONE", resource, sequence++);

            publishRunState(run, AgentTripStage.RISK_EVALUATION);
            Map<String, Object> riskInput = new LinkedHashMap<>();
            riskInput.put("intent", intent);
            riskInput.put("route", route);
            riskInput.put("resource", resource);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.RISK_EVALUATION, "评估风险",
                    "正在评估天气、拥堵、时间、换乘、步行距离和证件行李提醒。", "RUNNING", riskInput, sequence++);
            Map<String, Object> risk = riskEvaluationAgent.evaluate(riskInput);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.RISK_EVALUATION, "评估风险",
                    stringValue(risk.get("summary")), "DONE", risk, sequence++);

            publishRunState(run, AgentTripStage.CARD_GENERATION);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.CARD_GENERATION, "生成行程卡片",
                    "正在把候选资源转换成可确认的行程卡片。", "RUNNING", risk, sequence++);
            List<TripPlanCard> cards = tripCardGenerationAgent.generate(run.getId(), intent, route, resource, risk);
            for (TripPlanCard card : cards) {
                cardMapper.insert(card);
                eventPublisher.publishCard(emitter, card);
            }
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.CARD_GENERATION, "生成行程卡片",
                    "已生成行前准备、交通、接驳、酒店和到达提醒卡片。", "DONE", Map.of("cardCount", cards.size()), sequence++);

            publishRunState(run, AgentTripStage.USER_CONFIRMATION_WAITING);
            eventPublisher.publish(emitter, run.getId(), AgentTripStage.USER_CONFIRMATION_WAITING, "等待用户确认",
                    "需要确认的交通、接驳、酒店卡片不会自动加入最终行程单。", "DONE", Map.of("runId", run.getId()), sequence);
            run.setStatus("WAITING_CONFIRM");
            runMapper.updateById(run);
            eventPublisher.completed(emitter, run.getId(), "智能体规划完成，等待用户确认关键行程卡片。");
        } catch (Exception exception) {
            run.setStatus("FAILED");
            run.setCurrentStage("FAILED");
            runMapper.updateById(run);
            eventPublisher.failed(emitter, run.getId(), exception.getMessage());
        }
    }

    private void publishRunState(AgentRun run, String stage) {
        run.setCurrentStage(stage);
        run.setStatus("RUNNING");
        runMapper.updateById(run);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
