package com.itheima.travelagent2.modules.route.dto;

import com.itheima.travelagent2.modules.route.entity.Route;
import lombok.Data;

import java.util.List;

@Data
public class RoutePlanResponseDTO {
    private LocationInfo startLocation;
    private LocationInfo endLocation;
    private WeatherInfo startWeather;
    private WeatherInfo endWeather;
    private List<Route> routes;
    private List<String> serviceCapabilities;
}
