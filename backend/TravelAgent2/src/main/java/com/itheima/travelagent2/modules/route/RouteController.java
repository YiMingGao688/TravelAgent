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
/**
 * 交通规划接口层。
 * 前端提交起点、终点、偏好和可选坐标后，由 RouteService 对接高德路线、天气和 AI 推荐理由。
 */
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/query")
    /**
     * 查询路线规划结果。
     * 返回内容包含起终点坐标、adcode、天气、路线列表和服务能力，前端据此绘制地图与右侧方案。
     */
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
    /**
     * 删除路线记录。
     * 这里必须真实调用数据库删除，不能只返回成功，否则管理端/客户端会出现数据不同步。
     */
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = routeService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的路线记录");
        }
        return Result.success("Deleted");
    }
}
