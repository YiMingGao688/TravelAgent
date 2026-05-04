package com.itheima.travelagent2.modules.route.service;

import com.itheima.travelagent2.modules.route.dto.RouteQueryDTO;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.entity.Route;

import java.util.List;

public interface RouteService {
    RoutePlanResponseDTO queryRoutes(RouteQueryDTO dto);

    Route getRouteDetail(Long routeId);

    List<Route> listAll();

    boolean deleteById(Long id);
}
