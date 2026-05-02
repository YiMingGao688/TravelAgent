package com.itheima.travelagent2.modules.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.trip.dto.TripPlanSaveDTO;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.trip.mapper.TripPlanMapper;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripPlanServiceImpl implements TripPlanService {

    private final TripPlanMapper tripPlanMapper;

    public TripPlanServiceImpl(TripPlanMapper tripPlanMapper) {
        this.tripPlanMapper = tripPlanMapper;
    }

    @Override
    public TripPlan savePlan(TripPlanSaveDTO dto) {
        TripPlan plan = new TripPlan();
        plan.setUserId(trimToNull(dto.getUserId()));
        plan.setSessionId(trimToNull(dto.getSessionId()));
        plan.setTitle(defaultTitle(dto));
        plan.setStartPlace(trimToNull(dto.getStartPlace()));
        plan.setEndPlace(trimToNull(dto.getEndPlace()));
        plan.setContent(dto.getContent());
        plan.setRawQuestion(trimToNull(dto.getRawQuestion()));
        plan.setSourceType(trimToNull(dto.getSourceType()) == null ? "AI_CHAT" : dto.getSourceType());
        plan.setTravelPreference(trimToNull(dto.getTravelPreference()));
        plan.setBudget(trimToNull(dto.getBudget()));
        plan.setDays(dto.getDays());
        tripPlanMapper.insert(plan);
        return plan;
    }

    @Override
    public List<TripPlan> listByUserId(String userId) {
        LambdaQueryWrapper<TripPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripPlan::getUserId, userId).orderByDesc(TripPlan::getCreateTime);
        return tripPlanMapper.selectList(wrapper);
    }

    @Override
    public List<TripPlan> listAll() {
        LambdaQueryWrapper<TripPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TripPlan::getCreateTime);
        return tripPlanMapper.selectList(wrapper);
    }

    @Override
    public TripPlan getById(Long id) {
        return tripPlanMapper.selectById(id);
    }

    @Override
    public void deleteById(Long id) {
        tripPlanMapper.deleteById(id);
    }

    private String defaultTitle(TripPlanSaveDTO dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            return dto.getTitle().trim();
        }
        String start = trimToNull(dto.getStartPlace());
        String end = trimToNull(dto.getEndPlace());
        if (start != null && end != null) {
            return start + " -> " + end + " travel plan";
        }
        if (end != null) {
            return end + " travel plan";
        }
        return "travel plan";
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
