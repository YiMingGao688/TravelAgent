package com.itheima.travelagent2.modules.agentplan.agent;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.mapper.TripCandidateMapper;
import com.itheima.travelagent2.modules.agentplan.service.AgentStepRecorder;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TicketAgent {
    private final AgentStepRecorder recorder;
    private final TripCandidateMapper candidateMapper;

    public TicketAgent(AgentStepRecorder recorder, TripCandidateMapper candidateMapper) {
        this.recorder = recorder;
        this.candidateMapper = candidateMapper;
    }

    public int generate(Long sessionId, TravelChainResponseDTO response) {
        AgentStepRecord step = recorder.start(sessionId, "TicketAgent", "TICKET_CANDIDATE",
                "高铁/机票候选生成", "正在从可用交通产品中筛选票务候选", response.getTripSegments());
        List<TrainProduct> trains = response.getTrainProducts() == null ? List.of() : response.getTrainProducts();
        int count = 0;
        for (int i = 0; i < trains.size() && i < 3; i++) {
            TrainProduct train = trains.get(i);
            TripCandidate candidate = new TripCandidate();
            candidate.setSessionId(sessionId);
            candidate.setCandidateType("TRAIN");
            candidate.setTitle(train.getTrainNo() + " " + train.getStartStation() + " - " + train.getEndStation());
            candidate.setProvider(firstText(train.getDataSource(), "LOCAL_TRAIN_PRODUCT"));
            candidate.setStartPlace(train.getStartStation());
            candidate.setEndPlace(train.getEndStation());
            candidate.setStartTime(train.getDepartureTime());
            candidate.setEndTime(train.getArrivalTime());
            candidate.setPrice(train.getPrice());
            candidate.setScore(BigDecimal.valueOf(92 - i * 4L));
            candidate.setReason("匹配当前出发地、目的地和跨城行程分段，需用户确认后才加入行程单。");
            candidate.setDetailJson(recorder.toJson(train));
            candidate.setStatus("PENDING");
            candidateMapper.insert(candidate);
            count++;
        }
        recorder.success(step, "已生成 " + count + " 个票务候选，等待用户确认", java.util.Map.of("count", count));
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
