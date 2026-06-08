package com.itheima.travelagent2.modules.smartservice.dto;

import lombok.Data;

@Data
public class TravelReportRequestDTO {
    private String userId;
    private String startPlace;
    private String endPlace;
    private String travelDate;
    private String preference;
    private String specialNeed;
    private String aiPlan;
    private String routeSummary;
    private String weatherSummary;
    private String riskSummary;
    private String luggageInfo;
    private String afterSalesNeed;
    private String paymentPreference;
}
