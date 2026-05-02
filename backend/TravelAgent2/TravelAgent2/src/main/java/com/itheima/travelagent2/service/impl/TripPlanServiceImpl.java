package com.itheima.travelagent2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.dto.TripPlanDTO;
import com.itheima.travelagent2.entity.TripPlan;
import com.itheima.travelagent2.mapper.TripPlanMapper;
import com.itheima.travelagent2.service.TripPlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TripPlanServiceImpl extends ServiceImpl<TripPlanMapper, TripPlan> implements TripPlanService {

    @Override
    public TripPlan createTripPlan(TripPlanDTO dto) {
        TripPlan tripPlan = new TripPlan();
        tripPlan.setDestination(dto.getDestination());
        tripPlan.setStartDate(dto.getStartDate());
        tripPlan.setEndDate(dto.getEndDate());
        tripPlan.setTransportation(dto.getTransportation());
        tripPlan.setBudget(dto.getBudget());
        tripPlan.setItinerary(generateItinerary(dto));
        tripPlan.setStatus("planning");
        tripPlan.setCreateTime(LocalDateTime.now());
        tripPlan.setUpdateTime(LocalDateTime.now());
        this.save(tripPlan);
        return tripPlan;
    }

    @Override
    public List<TripPlan> getUserTripPlans(Long userId) {
        QueryWrapper<TripPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "planning");
        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public TripPlan getById(Long id) {
        return this.getById(id);
    }

    private String generateItinerary(TripPlanDTO dto) {
        return String.format("为您规划从%s到%s的%s日出行计划，预估预算%d元",
                LocalDateTime.now().toString().split("T")[0],
                dto.getDestination(),
                ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1,
                dto.getBudget());
    }
}