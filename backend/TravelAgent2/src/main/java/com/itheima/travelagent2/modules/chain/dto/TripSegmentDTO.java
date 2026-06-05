package com.itheima.travelagent2.modules.chain.dto;

import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TripSegmentDTO {
    private String segmentType;
    private String title;
    private String startPlace;
    private String endPlace;
    private String startLocation;
    private String endLocation;
    private String transportMode;
    private Integer durationMinutes;
    private Double distanceKm;
    private BigDecimal estimatedCost;
    private String routeSummary;
    private String routePolyline;
    private String source;
    private Long productId;
    private Object product;
    private List<RiskWarningDTO> riskWarnings = new ArrayList<>();
}
