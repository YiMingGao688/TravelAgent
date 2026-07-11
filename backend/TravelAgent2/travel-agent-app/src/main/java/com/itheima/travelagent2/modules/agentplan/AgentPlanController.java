package com.itheima.travelagent2.modules.agentplan;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanDetailDTO;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanStartRequestDTO;
import com.itheima.travelagent2.modules.agentplan.dto.AgentPlanSupplementRequestDTO;
import com.itheima.travelagent2.modules.agentplan.dto.CandidateActionRequestDTO;
import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.entity.TripItem;
import com.itheima.travelagent2.modules.agentplan.service.AgentPlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/plan")
public class AgentPlanController {
    private final AgentPlanService agentPlanService;

    public AgentPlanController(AgentPlanService agentPlanService) {
        this.agentPlanService = agentPlanService;
    }

    @PostMapping("/start")
    public Result<AgentPlanDetailDTO> start(@RequestBody AgentPlanStartRequestDTO request) {
        try {
            return Result.success(agentPlanService.start(request));
        } catch (Exception exception) {
            return Result.error(500, exception.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public Result<AgentPlanDetailDTO> detail(@PathVariable Long sessionId) {
        try {
            return Result.success(agentPlanService.detail(sessionId));
        } catch (Exception exception) {
            return Result.error(404, exception.getMessage());
        }
    }

    @PostMapping("/{sessionId}/supplement")
    public Result<AgentPlanDetailDTO> supplement(@PathVariable Long sessionId,
                                                 @RequestBody(required = false) AgentPlanSupplementRequestDTO request) {
        try {
            return Result.success(agentPlanService.supplement(sessionId, request));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }

    @GetMapping("/{sessionId}/steps")
    public Result<List<AgentStepRecord>> steps(@PathVariable Long sessionId) {
        return Result.success(agentPlanService.steps(sessionId));
    }

    @GetMapping("/{sessionId}/candidates")
    public Result<List<TripCandidate>> candidates(@PathVariable Long sessionId) {
        return Result.success(agentPlanService.candidates(sessionId));
    }

    @PostMapping("/{sessionId}/candidates/{candidateId}/confirm")
    public Result<AgentPlanDetailDTO> confirm(@PathVariable Long sessionId,
                                              @PathVariable Long candidateId,
                                              @RequestBody(required = false) CandidateActionRequestDTO request) {
        try {
            return Result.success(agentPlanService.confirm(sessionId, candidateId, request));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }

    @PostMapping("/{sessionId}/candidates/{candidateId}/reject")
    public Result<AgentPlanDetailDTO> reject(@PathVariable Long sessionId,
                                             @PathVariable Long candidateId,
                                             @RequestBody(required = false) CandidateActionRequestDTO request) {
        try {
            return Result.success(agentPlanService.reject(sessionId, candidateId, request));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }

    @PostMapping("/{sessionId}/candidates/{candidateId}/change")
    public Result<AgentPlanDetailDTO> change(@PathVariable Long sessionId,
                                             @PathVariable Long candidateId,
                                             @RequestBody(required = false) CandidateActionRequestDTO request) {
        try {
            return Result.success(agentPlanService.change(sessionId, candidateId, request));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }

    @GetMapping("/{sessionId}/items")
    public Result<List<TripItem>> items(@PathVariable Long sessionId) {
        return Result.success(agentPlanService.items(sessionId));
    }

    @PostMapping("/{sessionId}/finalize")
    public Result<AgentPlanDetailDTO> finalizePlan(@PathVariable Long sessionId) {
        try {
            return Result.success(agentPlanService.finalizePlan(sessionId));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }
}
