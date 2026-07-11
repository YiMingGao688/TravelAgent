package com.itheima.travelagent2.modules.agenttrip.dto;

import com.itheima.travelagent2.modules.agenttrip.entity.AgentEvent;
import com.itheima.travelagent2.modules.agenttrip.entity.AgentRun;
import com.itheima.travelagent2.modules.agenttrip.entity.FinalItineraryItem;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentTripRunDetailDTO {
    private AgentRun run;
    private List<AgentEvent> events = new ArrayList<>();
    private List<TripPlanCard> cards = new ArrayList<>();
    private List<FinalItineraryItem> finalItinerary = new ArrayList<>();
}
