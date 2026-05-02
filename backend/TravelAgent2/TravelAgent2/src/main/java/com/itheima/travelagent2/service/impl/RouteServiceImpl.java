package com.itheima.travelagent2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.dto.RouteQueryDTO;
import com.itheima.travelagent2.entity.Route;
import com.itheima.travelagent2.mapper.RouteMapper;
import com.itheima.travelagent2.service.RouteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements RouteService {

    @Override
    public List<Route> queryRoutes(RouteQueryDTO dto) {
        QueryWrapper<Route> wrapper = new QueryWrapper<>();
        if (dto.getStartPoint() != null && !dto.getStartPoint().isEmpty()) {
            wrapper.eq("start_point", dto.getStartPoint());
        }
        if (dto.getEndPoint() != null && !dto.getEndPoint().isEmpty()) {
            wrapper.eq("end_point", dto.getEndPoint());
        }
        if (dto.getRouteType() != null && !dto.getRouteType().isEmpty()) {
            wrapper.eq("route_type", dto.getRouteType());
        }
        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public Route getRouteDetail(Long routeId) {
        return this.getById(routeId);
    }
}