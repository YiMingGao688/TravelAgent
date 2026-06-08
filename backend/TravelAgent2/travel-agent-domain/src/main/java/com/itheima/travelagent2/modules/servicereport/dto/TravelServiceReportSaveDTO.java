package com.itheima.travelagent2.modules.servicereport.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class TravelServiceReportSaveDTO {
    private String userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tripPlanId;
    private String title;
    private String startPlace;
    private String endPlace;
    private String reportContent;
    private String reportSummary;
    private String sourceType;
}
