package com.itheima.travelagent2.modules.agenttrip.dto;

import com.itheima.travelagent2.modules.agenttrip.entity.FinalItineraryItem;
import com.itheima.travelagent2.modules.agenttrip.entity.TripPlanCard;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CardConfirmResponseDTO {
    private TripPlanCard card;
    private List<FinalItineraryItem> finalItinerary = new ArrayList<>();
}
