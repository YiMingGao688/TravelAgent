package com.itheima.travelagent2.controller;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.dto.TripPlanDTO;
import com.itheima.travelagent2.entity.TripPlan;
import com.itheima.travelagent2.service.TripPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip")
public class TripPlanController {

    @Autowired
    private TripPlanService tripPlanService;

    @PostMapping("/create")
    public Result<TripPlan> createTripPlan(@RequestBody TripPlanDTO dto) {
        if (dto.getDestination() == null || dto.getDestination().isEmpty()) {
            return Result.error(400, "目的地不能为空");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            return Result.error(400, "开始时间和结束时间不能为空");
        }
        TripPlan tripPlan = tripPlanService.createTripPlan(dto);
        return Result.success("出行规划创建成功", tripPlan);
    }

    @GetMapping("/list")
    public Result<List<TripPlan>> getTripPlans() {
        List<TripPlan> plans = tripPlanService.getUserTripPlans(1L);
        return Result.success(plans);
    }

    @GetMapping("/{id}")
    public Result<TripPlan> getTripPlanDetail(@PathVariable Long id) {
        TripPlan tripPlan = tripPlanService.getById(id);
        if (tripPlan == null) {
            return Result.error(404, "出行规划不存在");
        }
        return Result.success(tripPlan);
    }
}
