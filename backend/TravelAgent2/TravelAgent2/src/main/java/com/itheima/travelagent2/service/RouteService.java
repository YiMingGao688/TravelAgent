package com.itheima.travelagent2.service;

import com.itheima.travelagent2.dto.RouteQueryDTO;
import com.itheima.travelagent2.entity.Route;
import java.util.List;

public interface RouteService {
    List<Route> queryRoutes(RouteQueryDTO dto);
    Route getRouteDetail(Long routeId);
}