package com.itheima.travelagent2.modules.trip.dto;

import lombok.Data;

@Data
public class TripPlanSaveDTO {
    private String userId;
    private String sessionId;
    private String title;
    private String startPlace;
    private String endPlace;
    private String content;
    private String rawQuestion;
    private String sourceType;
    private String travelPreference;
    private String budget;
    private Integer days;
}
