package com.itheima.travelagent2.modules.trip.service;

import com.itheima.travelagent2.modules.trip.dto.TripPlanSaveDTO;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;

import java.util.List;

public interface TripPlanService {
    TripPlan savePlan(TripPlanSaveDTO dto);

    TripPlan updatePlan(Long id, TripPlanSaveDTO dto);

    List<TripPlan> listByUserId(String userId);

    List<TripPlan> listAll();

    TripPlan getById(Long id);

    boolean deleteById(Long id);
}
