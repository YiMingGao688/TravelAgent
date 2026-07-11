package com.itheima.travelagent2.modules.agentplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.agentplan.agent.ReportAgent;
import com.itheima.travelagent2.modules.agentplan.agent.TravelMasterAgent;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanDetailDTO;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanStartRequestDTO;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanSupplementRequestDTO;
import com.itheima.travelagent2.modules.agentplan.dto.CandidateActionRequestDTO;
import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.PlanSession;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.entity.TripItem;
import com.itheima.travelagent2.modules.agentplan.entity.TripPlanSummary;
import com.itheima.travelagent2.modules.agentplan.entity.UserConfirmAction;
import com.itheima.travelagent2.modules.agentplan.mapper.AgentStepRecordMapper;
import com.itheima.travelagent2.modules.agentplan.mapper.PlanSessionMapper;
import com.itheima.travelagent2.modules.agentplan.mapper.TripCandidateMapper;
import com.itheima.travelagent2.modules.agentplan.mapper.TripItemMapper;
import com.itheima.travelagent2.modules.agentplan.mapper.TripPlanSummaryMapper;
import com.itheima.travelagent2.modules.agentplan.mapper.UserConfirmActionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentPlanService {
    private final PlanSessionMapper sessionMapper;
    private final AgentStepRecordMapper stepMapper;
    private final TripCandidateMapper candidateMapper;
    private final TripItemMapper itemMapper;
    private final UserConfirmActionMapper actionMapper;
    private final TripPlanSummaryMapper summaryMapper;
    private final TravelMasterAgent masterAgent;
    private final ReportAgent reportAgent;
    private final AgentStepRecorder stepRecorder;
    private final ObjectMapper objectMapper;

    public AgentPlanService(PlanSessionMapper sessionMapper,
                            AgentStepRecordMapper stepMapper,
                            TripCandidateMapper candidateMapper,
                            TripItemMapper itemMapper,
                            UserConfirmActionMapper actionMapper,
                            TripPlanSummaryMapper summaryMapper,
                            TravelMasterAgent masterAgent,
                            ReportAgent reportAgent,
                            AgentStepRecorder stepRecorder,
                            ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.stepMapper = stepMapper;
        this.candidateMapper = candidateMapper;
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
        this.summaryMapper = summaryMapper;
        this.masterAgent = masterAgent;
        this.reportAgent = reportAgent;
        this.stepRecorder = stepRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentPlanDetailDTO start(AgentPlanStartRequestDTO request) {
        PlanSession session = masterAgent.start(request);
        return detail(session.getId());
    }

    public AgentPlanDetailDTO detail(Long sessionId) {
        PlanSession session = requireSession(sessionId);
        AgentPlanDetailDTO detail = new AgentPlanDetailDTO();
        detail.setSession(session);
        detail.setSteps(steps(sessionId));
        detail.setCandidates(candidates(sessionId));
        detail.setItems(items(sessionId));
        detail.setSummary(summary(sessionId));
        detail.setReadyToFinalize(!detail.getItems().isEmpty()
                && detail.getCandidates().stream().noneMatch(item -> "PENDING".equals(item.getStatus())));
        return detail;
    }

    @Transactional
    public AgentPlanDetailDTO supplement(Long sessionId, AgentPlanSupplementRequestDTO request) {
        PlanSession session = requireSession(sessionId);
        AgentStepRecord step = stepRecorder.start(
                sessionId,
                "TravelMasterAgent",
                "USER_SUPPLEMENT",
                "User supplement",
                "User supplied missing planning fields. Continue current session without restarting.",
                request);
        if (request != null) {
            if (request.getDepartTime() != null) {
                session.setDepartTime(request.getDepartTime());
            }
            if (request.getReturnTime() != null) {
                session.setReturnTime(request.getReturnTime());
            }
            if (request.getBudget() != null) {
                session.setBudget(request.getBudget());
            }
            if (request.getPreference() != null && !request.getPreference().isBlank()) {
                session.setPreference(mergeText(session.getPreference(), request.getPreference()));
            }
        }
        session.setStatus("WAITING_CONFIRM");
        session.setCurrentStep("USER_SUPPLEMENT");
        sessionMapper.updateById(session);
        stepRecorder.success(step, "Supplement saved into current plan session.", session);
        return detail(sessionId);
    }

    public List<AgentStepRecord> steps(Long sessionId) {
        return stepMapper.selectList(new LambdaQueryWrapper<AgentStepRecord>()
                .eq(AgentStepRecord::getSessionId, sessionId)
                .orderByAsc(AgentStepRecord::getStartedAt)
                .orderByAsc(AgentStepRecord::getId));
    }

    public List<TripCandidate> candidates(Long sessionId) {
        return candidateMapper.selectList(new LambdaQueryWrapper<TripCandidate>()
                .eq(TripCandidate::getSessionId, sessionId)
                .orderByAsc(TripCandidate::getCreatedAt)
                .orderByAsc(TripCandidate::getId));
    }

    public List<TripItem> items(Long sessionId) {
        return itemMapper.selectList(new LambdaQueryWrapper<TripItem>()
                .eq(TripItem::getSessionId, sessionId)
                .orderByAsc(TripItem::getSortOrder)
                .orderByAsc(TripItem::getId));
    }

    @Transactional
    public AgentPlanDetailDTO confirm(Long sessionId, Long candidateId, CandidateActionRequestDTO request) {
        requireSession(sessionId);
        TripCandidate candidate = requireCandidate(sessionId, candidateId);
        if (!"PENDING".equals(candidate.getStatus())) {
            throw new IllegalStateException("候选项当前状态为 " + candidate.getStatus() + "，不能重复确认");
        }
        candidate.setStatus("SELECTED");
        candidateMapper.updateById(candidate);

        TripItem item = new TripItem();
        item.setSessionId(sessionId);
        item.setSourceCandidateId(candidateId);
        item.setItemType(candidate.getCandidateType());
        item.setTitle(candidate.getTitle());
        item.setStartPlace(candidate.getStartPlace());
        item.setEndPlace(candidate.getEndPlace());
        item.setStartTime(candidate.getStartTime());
        item.setEndTime(candidate.getEndTime());
        item.setPrice(candidate.getPrice());
        item.setStatus("CONFIRMED");
        item.setDetailJson(candidate.getDetailJson());
        item.setSortOrder(nextSortOrder(sessionId));
        itemMapper.insert(item);

        recordAction(sessionId, candidateId, "CONFIRM", "SELECTED", request == null ? null : request.getRemark());
        return detail(sessionId);
    }

    @Transactional
    public AgentPlanDetailDTO reject(Long sessionId, Long candidateId, CandidateActionRequestDTO request) {
        requireSession(sessionId);
        TripCandidate candidate = requireCandidate(sessionId, candidateId);
        if ("PENDING".equals(candidate.getStatus())) {
            candidate.setStatus("REJECTED");
            candidateMapper.updateById(candidate);
        }
        recordAction(sessionId, candidateId, "REJECT", "REJECTED", request == null ? null : request.getRemark());
        return detail(sessionId);
    }

    @Transactional
    public AgentPlanDetailDTO change(Long sessionId, Long candidateId, CandidateActionRequestDTO request) {
        requireSession(sessionId);
        TripCandidate candidate = requireCandidate(sessionId, candidateId);
        if ("PENDING".equals(candidate.getStatus())) {
            candidate.setStatus("EXPIRED");
            candidateMapper.updateById(candidate);
        }
        recordAction(sessionId, candidateId, "CHANGE", "REQUESTED",
                request == null ? null : firstText(request.getChangeReason(), request.getRemark()));
        masterAgent.regenerateCandidates(sessionId, initialRequest(sessionId), candidate.getCandidateType());
        return detail(sessionId);
    }

    @Transactional
    public AgentPlanDetailDTO finalizePlan(Long sessionId) {
        PlanSession session = requireSession(sessionId);
        List<TripItem> confirmedItems = items(sessionId);
        if (confirmedItems.isEmpty()) {
            throw new IllegalStateException("请至少确认一个候选项后再生成总行程卡片");
        }
        TripPlanSummary summary = summary(sessionId);
        reportAgent.generate(session, confirmedItems, summary);
        session.setStatus("SUMMARY_READY");
        session.setCurrentStep("FINAL_SUMMARY");
        sessionMapper.updateById(session);
        return detail(sessionId);
    }

    private PlanSession requireSession(Long sessionId) {
        PlanSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("未找到规划会话：" + sessionId);
        }
        return session;
    }

    private TripCandidate requireCandidate(Long sessionId, Long candidateId) {
        TripCandidate candidate = candidateMapper.selectOne(new LambdaQueryWrapper<TripCandidate>()
                .eq(TripCandidate::getSessionId, sessionId)
                .eq(TripCandidate::getId, candidateId));
        if (candidate == null) {
            throw new IllegalArgumentException("未找到候选项：" + candidateId);
        }
        return candidate;
    }

    private TripPlanSummary summary(Long sessionId) {
        return summaryMapper.selectOne(new LambdaQueryWrapper<TripPlanSummary>()
                .eq(TripPlanSummary::getSessionId, sessionId)
                .last("LIMIT 1"));
    }

    private int nextSortOrder(Long sessionId) {
        TripItem latest = itemMapper.selectOne(new LambdaQueryWrapper<TripItem>()
                .eq(TripItem::getSessionId, sessionId)
                .orderByDesc(TripItem::getSortOrder)
                .last("LIMIT 1"));
        return latest == null || latest.getSortOrder() == null ? 10 : latest.getSortOrder() + 10;
    }

    private void recordAction(Long sessionId, Long candidateId, String type, String result, String remark) {
        UserConfirmAction action = new UserConfirmAction();
        action.setSessionId(sessionId);
        action.setCandidateId(candidateId);
        action.setActionType(type);
        action.setActionResult(result);
        action.setRemark(remark);
        actionMapper.insert(action);
    }

    private AgentPlanStartRequestDTO initialRequest(Long sessionId) {
        AgentStepRecord step = stepMapper.selectOne(new LambdaQueryWrapper<AgentStepRecord>()
                .eq(AgentStepRecord::getSessionId, sessionId)
                .eq(AgentStepRecord::getStepKey, "MASTER_START")
                .orderByAsc(AgentStepRecord::getId)
                .last("LIMIT 1"));
        if (step == null || step.getInputJson() == null || step.getInputJson().isBlank()) {
            throw new IllegalStateException("缺少原始规划请求，无法换方案");
        }
        try {
            return objectMapper.readValue(step.getInputJson(), AgentPlanStartRequestDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("原始规划请求解析失败，无法换方案", exception);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String mergeText(String source, String addition) {
        if (source == null || source.isBlank()) {
            return addition.trim();
        }
        if (source.contains(addition.trim())) {
            return source;
        }
        return source + "; " + addition.trim();
    }
}
