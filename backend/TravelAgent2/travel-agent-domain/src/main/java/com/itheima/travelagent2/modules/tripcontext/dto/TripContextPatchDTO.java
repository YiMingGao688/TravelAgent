package com.itheima.travelagent2.modules.tripcontext.dto;

import lombok.Data;

@Data
public class TripContextPatchDTO {
    private String startPlace;
    private String endPlace;
    private String sceneType;
    private Integer days;
    private String preference;
    private String budget;
    private String companionType;
    private String transportMode;
    private String pacePreference;
    private String walkingPreference;
    private String hotelPreference;
    private String foodPreference;
    private String specialNeeds;
}
