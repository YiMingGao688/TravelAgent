package com.itheima.travelagent2.modules.route.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class RouteQueryDTO {
    private String userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tripPlanId;
    private String startPoint;
    private String endPoint;
    private String startLocation;
    private String endLocation;
    private String startAdcode;
    private String endAdcode;
    private String startCity;
    private String endCity;
    private String routeType;
    private String preference;
}
