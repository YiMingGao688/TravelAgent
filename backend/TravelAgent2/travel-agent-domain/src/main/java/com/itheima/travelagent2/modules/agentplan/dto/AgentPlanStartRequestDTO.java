package com.itheima.travelagent2.modules.agentplan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentPlanStartRequestDTO {
    private String userId;
    private String sessionTitle;
    private String startPlace;
    private String endPlace;
    private String startLocation;
    private String endLocation;
    private String startAdcode;
    private String endAdcode;
    private String startCity;
    private String endCity;
    private LocalDateTime departTime;
    private LocalDateTime returnTime;
    private String sceneType;
    private String preference;
    private BigDecimal budget;
    private String message;
}
