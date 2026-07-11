package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.mapper.TripCandidateMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RouteAgent {
    private final AgentStepRecorder recorder;
    private final TripCandidateMapper candidateMapper;

    public RouteAgent(AgentStepRecorder recorder, TripCandidateMapper candidateMapper) {
        this.recorder = recorder;
        this.candidateMapper = candidateMapper;
    }

    public int generate(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "RouteAgent", "ROUTE_CANDIDATE",
                "路线候选生成", "正在生成接驳、公交、地铁、打车等路线候选", response.getGeoInfo());
        List<Route> routes = response.getRoutePlans() == null ? List.of() : response.getRoutePlans();
        int count = 0;
        for (int i = 0; i < routes.size() && i < 3; i++) {
            Route route = routes.get(i);
            TripCandidate candidate = new TripCandidate();
            candidate.setSessionId(sessionId);
            candidate.setCandidateType(routeType(i));
            candidate.setTitle(firstText(route.getRouteName(), "智能路线候选"));
            candidate.setProvider("AMAP_ROUTE");
            candidate.setStartPlace(route.getStartPoint());
            candidate.setEndPlace(route.getEndPoint());
            candidate.setPrice(route.getCost() == null ? null : BigDecimal.valueOf(route.getCost()));
            candidate.setScore(BigDecimal.valueOf(90 - i * 3L));
            candidate.setReason(firstText(route.getRecommendReason(), route.getDescription()));
            candidate.setDetailJson(recorder.toJson(route));
            candidate.setStatus("PENDING");
            candidateMapper.insert(candidate);
            count++;
        }
        recorder.success(step, "已生成 " + count + " 个路线候选，等待用户确认是否加入行程", java.util.Map.of("count", count));
        return count;
    }

    private String routeType(int index) {
        return switch (index) {
            case 1 -> "METRO";
            case 2 -> "BUS";
            default -> "TAXI";
        };
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
