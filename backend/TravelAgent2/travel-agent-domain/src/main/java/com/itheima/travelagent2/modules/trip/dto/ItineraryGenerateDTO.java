package com.itheima.travelagent2.modules.trip.dto;

import lombok.Data;

@Data
public class ItineraryGenerateDTO {
    private String userId;
    private String sessionId;
    private String startPlace;
    private String destination;
    private Integer days;
    private String travelDate;
    private String crowdType;
    private String preference;
    private String budget;
}
