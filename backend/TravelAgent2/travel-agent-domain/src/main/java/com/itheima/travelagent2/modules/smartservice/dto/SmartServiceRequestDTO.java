package com.itheima.travelagent2.modules.smartservice.dto;

import lombok.Data;

@Data
public class SmartServiceRequestDTO {
    private String userId;
    private String serviceType;
    private String startPlace;
    private String endPlace;
    private String travelDate;
    private String preference;
    private String specialNeed;
    private String budget;
    private String passengerInfo;
    private String luggageInfo;
    private String paymentPreference;
    private String scenario;
    private String extraRequirement;
}
