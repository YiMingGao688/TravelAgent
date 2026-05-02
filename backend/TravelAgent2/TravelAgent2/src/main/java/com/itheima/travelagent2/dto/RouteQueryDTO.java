package com.itheima.travelagent2.dto;

import lombok.Data;

@Data
public class RouteQueryDTO {
    private String startPoint;
    private String endPoint;
    private String routeType;
}
