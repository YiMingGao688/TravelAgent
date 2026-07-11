package com.itheima.travelagent2.modules.agenttrip;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.agenttrip.dto.AgentTripRunDetailDTO;
import com.itheima.travelagent2.modules.agenttrip.dto.CardConfirmResponseDTO;
import com.itheima.travelagent2.modules.agenttrip.dto.TripPlanStreamRequestDTO;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentEvent;
import com.itheima.travelagent2.modules.agenttrip.entity.FinalItineraryItem;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import com.itheima.travelagent2.modules.agenttrip.service.TravelPlanningAgentService;
import com.itheima.travelagent2.modules.agenttrip.service.TripCardConfirmService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/agent/trip")
public class AgentTripController {
    private final TravelPlanningAgentService planningAgentService;
    private final TripCardConfirmService cardConfirmService;

    public AgentTripController(TravelPlanningAgentService planningAgentService,
                               TripCardConfirmService cardConfirmService) {
        this.planningAgentService = planningAgentService;
        this.cardConfirmService = cardConfirmService;
    }

    @PostMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPlan(@RequestBody TripPlanStreamRequestDTO request) {
        return planningAgentService.streamPlan(request);
    }

    @PostMapping("/card/{cardId}/confirm")
    public Result<CardConfirmResponseDTO> confirmCard(@PathVariable Long cardId) {
        try {
            return Result.success(cardConfirmService.confirm(cardId));
        } catch (Exception exception) {
            return Result.error(400, exception.getMessage());
        }
    }

    @GetMapping("/run/{runId}")
    public Result<AgentTripRunDetailDTO> detail(@PathVariable Long runId) {
        return Result.success(planningAgentService.detail(runId));
    }

    @GetMapping("/run/{runId}/events")
    public Result<List<AgentEvent>> events(@PathVariable Long runId) {
        return Result.success(planningAgentService.events(runId));
    }

    @GetMapping("/run/{runId}/cards")
    public Result<List<TripPlanCard>> cards(@PathVariable Long runId) {
        return Result.success(planningAgentService.cards(runId));
    }

    @GetMapping("/run/{runId}/final-itinerary")
    public Result<List<FinalItineraryItem>> finalItinerary(@PathVariable Long runId) {
        return Result.success(cardConfirmService.listFinal(runId));
    }
}
