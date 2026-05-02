package com.itheima.travelagent2.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TripPlanDTO {
    private String destination;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String transportation;
    private Integer budget;
}
