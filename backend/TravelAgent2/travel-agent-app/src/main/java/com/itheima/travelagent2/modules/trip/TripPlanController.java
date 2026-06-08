package com.itheima.travelagent2.modules.trip;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.trip.dto.TripPlanSaveDTO;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trip")
/**
 * 历史方案/我的行程接口。
 * 客户端和管理端都应通过这里真实保存、查询、更新、删除 trip_plan 数据。
 */
public class TripPlanController {

    private final TripPlanService tripPlanService;

    public TripPlanController(TripPlanService tripPlanService) {
        this.tripPlanService = tripPlanService;
    }

    @PostMapping("/save")
    /**
     * 保存 AI 咨询或行程定制生成的方案。
     * 前端传入的结构化字段会落到 trip_plan，便于后续一键导入交通规划和服务报告。
     */
    public Result<TripPlan> savePlan(@RequestBody TripPlanSaveDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            return Result.error(400, "User id is required");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            return Result.error(400, "Plan content is required");
        }
        return Result.success("Saved", tripPlanService.savePlan(dto));
    }

    @PostMapping
    public Result<TripPlan> create(@RequestBody TripPlanSaveDTO dto) {
        return savePlan(dto);
    }

    @GetMapping("/list")
    /**
     * 按用户查询历史方案列表。
     * 列表必须以后端数据库为准，前端删除或新增后会重新调用该接口刷新。
     */
    public Result<List<TripPlan>> listByUserId(@RequestParam String userId) {
        if (userId == null || userId.isBlank()) {
            return Result.error(400, "User id is required");
        }
        return Result.success(tripPlanService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<TripPlan> detail(@PathVariable Long id) {
        TripPlan plan = tripPlanService.getById(id);
        if (plan == null) {
            return Result.error(404, "Plan not found");
        }
        return Result.success(plan);
    }

    @PutMapping("/{id}")
    public Result<TripPlan> update(@PathVariable Long id, @RequestBody TripPlanSaveDTO dto) {
        TripPlan plan = tripPlanService.updatePlan(id, dto);
        if (plan == null) {
            return Result.error(404, "Plan not found");
        }
        return Result.success("Updated", plan);
    }

    @DeleteMapping("/{id}")
    /**
     * 删除历史方案。
     * 前端 Long 雪花 ID 会以字符串形式传递，Spring 自动转 Long 后执行真实数据库删除。
     */
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = tripPlanService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的历史方案");
        }
        return Result.success("Deleted");
    }
}
