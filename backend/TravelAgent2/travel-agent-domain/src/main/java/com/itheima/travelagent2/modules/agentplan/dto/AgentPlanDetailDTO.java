package com.itheima.travelagent2.modules.agentplan.dto;

import com.itheima.travelagent2.modules.agentplan.entity.AgentStepRecord;
import com.itheima.travelagent2.modules.agentplan.entity.PlanSession;
import com.itheima.travelagent2.modules.agentplan.entity.TripCandidate;
import com.itheima.travelagent2.modules.agentplan.entity.TripItem;
import com.itheima.travelagent2.modules.agentplan.entity.TripPlanSummary;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentPlanDetailDTO {
    private PlanSession session;
    private List<AgentStepRecord> steps = new ArrayList<>();
    private List<TripCandidate> candidates = new ArrayList<>();
    private List<TripItem> items = new ArrayList<>();
    private TripPlanSummary summary;
    private boolean readyToFinalize;
}
