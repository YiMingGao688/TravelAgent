package com.itheima.travelagent2.modules.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.trip.dto.TripPlanSaveDTO;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.trip.mapper.TripPlanMapper;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import com.itheima.travelagent2.modules.tripcontext.entity.TripContext;
import com.itheima.travelagent2.modules.tripcontext.service.TripContextService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripPlanServiceImpl implements TripPlanService {

    private final TripPlanMapper tripPlanMapper;
    private final TripContextService tripContextService;

    public TripPlanServiceImpl(TripPlanMapper tripPlanMapper, TripContextService tripContextService) {
        this.tripPlanMapper = tripPlanMapper;
        this.tripContextService = tripContextService;
    }

    @Override
    public TripPlan savePlan(TripPlanSaveDTO dto) {
        TripPlan plan = new TripPlan();
        apply(plan, dto);
        tripPlanMapper.insert(plan);
        return plan;
    }

    @Override
    public TripPlan updatePlan(Long id, TripPlanSaveDTO dto) {
        TripPlan plan = tripPlanMapper.selectById(id);
        if (plan == null) {
            return null;
        }
        apply(plan, dto);
        tripPlanMapper.updateById(plan);
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
    public boolean deleteById(Long id) {
        return tripPlanMapper.deleteById(id) > 0;
    }

    private String defaultTitle(TripPlanSaveDTO dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            return dto.getTitle().trim();
        }
        String start = trimToNull(dto.getStartPlace());
        String end = trimToNull(dto.getEndPlace());
        if (start != null && end != null) {
            return start + " 到 " + end + " 出行方案";
        }
        if (end != null) {
            return end + " 出行方案";
        }
        return "智能出行方案";
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void apply(TripPlan plan, TripPlanSaveDTO dto) {
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

        TripContext context = tripContextService.getBySessionId(dto.getSessionId());
        if (context != null) {
            if (trimToNull(context.getStartPlace()) != null) plan.setStartPlace(context.getStartPlace());
            if (trimToNull(context.getEndPlace()) != null) plan.setEndPlace(context.getEndPlace());
            if (trimToNull(context.getPreference()) != null) plan.setTravelPreference(context.getPreference());
            if (trimToNull(context.getBudget()) != null) plan.setBudget(context.getBudget());
            if (context.getDays() != null) plan.setDays(context.getDays());
            if (trimToNull(dto.getTitle()) == null) {
                plan.setTitle(defaultTitleFromContext(context));
            }
        }
    }

    private String defaultTitleFromContext(TripContext context) {
        String start = trimToNull(context.getStartPlace());
        String end = trimToNull(context.getEndPlace());
        Integer days = context.getDays();
        String preference = trimToNull(context.getPreference());
        if (start != null && end != null) {
            return start + " 到 " + end + (days == null ? "" : days + "天") + (preference == null ? "" : preference) + "出行方案";
        }
        return defaultTitle(new TripPlanSaveDTO());
    }
}
