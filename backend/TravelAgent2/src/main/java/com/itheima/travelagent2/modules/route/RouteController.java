package com.itheima.travelagent2.modules.route;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.route.dto.RouteQueryDTO;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.route.service.RouteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/query")
    public Result<RoutePlanResponseDTO> queryRoutes(@RequestBody RouteQueryDTO dto) {
        if (dto.getStartPoint() == null || dto.getStartPoint().isBlank()) {
            return Result.error(400, "Start point is required");
        }
        if (dto.getEndPoint() == null || dto.getEndPoint().isBlank()) {
            return Result.error(400, "End point is required");
        }
        try {
            return Result.success("OK", routeService.queryRoutes(dto));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Route> getRouteDetail(@PathVariable Long id) {
        Route route = routeService.getRouteDetail(id);
        if (route == null) {
            return Result.error(404, "Route not found");
        }
        return Result.success(route);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = routeService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的路线记录");
        }
        return Result.success("Deleted");
    }
}
