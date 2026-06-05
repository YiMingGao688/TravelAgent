package com.itheima.travelagent2.modules.amap.dto;

import lombok.Data;

@Data
public class AmapRouteResult {
    private String mode;
    private String startLocation;
    private String endLocation;
    private Double distanceKm;
    private Integer durationMinutes;
    private Double estimatedCost;
    private String routeSummary;
    private String source = "AMAP";
    private String rawPolyline;
}
