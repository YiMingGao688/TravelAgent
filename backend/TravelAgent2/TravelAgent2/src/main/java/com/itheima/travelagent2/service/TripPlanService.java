package com.itheima.travelagent2.service;

import com.itheima.travelagent2.dto.TripPlanDTO;
import com.itheima.travelagent2.entity.TripPlan;
import java.util.List;

public interface TripPlanService {
    TripPlan createTripPlan(TripPlanDTO dto);
    List<TripPlan> getUserTripPlans(Long userId);
    TripPlan getById(Long id);
}