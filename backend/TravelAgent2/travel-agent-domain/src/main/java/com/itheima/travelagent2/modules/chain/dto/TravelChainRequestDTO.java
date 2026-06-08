package com.itheima.travelagent2.modules.chain.dto;

import lombok.Data;

@Data
public class TravelChainRequestDTO {
    private String userId;
    private String sessionId;
    private String startPlace;
    private String endPlace;
    private String currentLocation;
    private String startLocation;
    private String endLocation;
    private String startAdcode;
    private String endAdcode;
    private String startCity;
    private String endCity;
    private String message;
}
