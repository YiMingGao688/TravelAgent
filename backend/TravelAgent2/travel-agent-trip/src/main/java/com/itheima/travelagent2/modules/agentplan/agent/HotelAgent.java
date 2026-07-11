package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.mapper.TripCandidateMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class HotelAgent {
    private final AgentStepRecorder recorder;
    private final TripCandidateMapper candidateMapper;

    public HotelAgent(AgentStepRecorder recorder, TripCandidateMapper candidateMapper) {
        this.recorder = recorder;
        this.candidateMapper = candidateMapper;
    }

    public int generate(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "HotelAgent", "HOTEL_CANDIDATE",
                "酒店候选生成", "正在根据目的地和预算筛选酒店候选", response.getDemand());
        List<HotelItemDTO> hotels = response.getHotels() == null ? List.of() : response.getHotels();
        int count = 0;
        for (int i = 0; i < hotels.size() && i < 3; i++) {
            HotelItemDTO hotel = hotels.get(i);
            TripCandidate candidate = new TripCandidate();
            candidate.setSessionId(sessionId);
            candidate.setCandidateType("HOTEL");
            candidate.setTitle(firstText(hotel.getName(), hotel.getHotelName(), "酒店候选"));
            candidate.setProvider(firstText(hotel.getSource(), "HOTEL_SERVICE"));
            candidate.setStartPlace(response.getDemand() == null ? null : response.getDemand().getEndPlace());
            candidate.setEndPlace(hotel.getAddress());
            candidate.setPrice(firstMoney(hotel.getLowestPrice(), hotel.getMinPrice()));
            candidate.setScore(firstMoney(hotel.getScore(), hotel.getRating(), BigDecimal.valueOf(4.5)));
            candidate.setReason(firstText(hotel.getDescription(), "匹配目的地附近住宿需求，需用户确认后才加入行程单。"));
            candidate.setDetailJson(recorder.toJson(hotel));
            candidate.setStatus("PENDING");
            candidateMapper.insert(candidate);
            count++;
        }
        recorder.success(step, "已生成 " + count + " 个酒店候选，等待用户确认", java.util.Map.of("count", count));
        return count;
    }

    private BigDecimal firstMoney(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
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
