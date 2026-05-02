package com.itheima.travelagent2.controller;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.dto.RouteQueryDTO;
import com.itheima.travelagent2.entity.Route;
import com.itheima.travelagent2.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/query")
    public Result<List<Route>> queryRoutes(@RequestBody RouteQueryDTO dto) {
        List<Route> routes = routeService.queryRoutes(dto);
        return Result.success("查询成功", routes);
    }

    @GetMapping("/{id}")
    public Result<Route> getRouteDetail(@PathVariable Long id) {
        Route route = routeService.getRouteDetail(id);
        if (route == null) {
            return Result.error(404, "路线不存在");
        }
        return Result.success(route);
    }
}
