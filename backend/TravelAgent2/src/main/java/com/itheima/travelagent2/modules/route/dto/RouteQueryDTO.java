package com.itheima.travelagent2.modules.route.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
/**
 * 交通规划请求参数。
 * startPoint/endPoint 是用户输入的名称；startLocation/endLocation 是前端候选地点带回的坐标，
 * 有坐标时后端会优先使用坐标，减少高德地理编码失败。
 */
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
    private List<String> waypoints = new ArrayList<>();
    private String routeType;
    private String preference;
}
