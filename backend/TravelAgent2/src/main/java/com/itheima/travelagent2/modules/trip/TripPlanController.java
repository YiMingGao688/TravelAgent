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
public class TripPlanController {

    private final TripPlanService tripPlanService;

    public TripPlanController(TripPlanService tripPlanService) {
        this.tripPlanService = tripPlanService;
    }

    @PostMapping("/save")
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
    public Result<String> delete(@PathVariable Long id) {
        System.out.println("后端收到删除 id = " + id);
        boolean removed = tripPlanService.deleteById(id);
        if (!removed) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的历史方案");
        }
        return Result.success("Deleted");
    }
}
