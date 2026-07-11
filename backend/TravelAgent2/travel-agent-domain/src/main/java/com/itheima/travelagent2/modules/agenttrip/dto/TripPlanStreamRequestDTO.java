package com.itheima.travelagent2.modules.agenttrip.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TripPlanStreamRequestDTO {
    private String userId;
    private String requestId;
    private String startPlace;
    private String endPlace;
    private String requirementText;
    private String departDate;
    private BigDecimal budget;
    private String preference;
    private Integer peopleCount;
    private String sceneType;
}
