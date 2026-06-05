package com.itheima.travelagent2.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import com.itheima.travelagent2.modules.booking.mapper.TrafficProductMapper;
import com.itheima.travelagent2.modules.booking.service.TrafficProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrafficProductServiceImpl extends ServiceImpl<TrafficProductMapper, TrafficProduct> implements TrafficProductService {

    private static final Logger log = LoggerFactory.getLogger(TrafficProductServiceImpl.class);

    @Override
    public List<TrafficProduct> search(String type, String startPlace, String endPlace, String departDate) {
        LambdaQueryWrapper<TrafficProduct> wrapper = new LambdaQueryWrapper<>();
        if (hasText(type)) {
            wrapper.eq(TrafficProduct::getType, type);
        }
        if (hasText(startPlace)) {
            wrapper.like(TrafficProduct::getStartPlace, startPlace);
        }
        if (hasText(endPlace)) {
            wrapper.like(TrafficProduct::getEndPlace, endPlace);
        }
        if (hasText(departDate)) {
            LocalDate date = LocalDate.parse(departDate);
            wrapper.between(TrafficProduct::getDepartTime, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        }
        wrapper.orderByAsc(TrafficProduct::getDepartTime).orderByAsc(TrafficProduct::getPrice);
        return filterDirty(list(wrapper));
    }

    @Override
    public List<TrafficProduct> listAll() {
        return filterDirty(list(new LambdaQueryWrapper<TrafficProduct>().orderByDesc(TrafficProduct::getDepartTime)));
    }

    /**
     * 过滤脏数据：arrive_time 必须大于 depart_time。
     * 发现脏数据时记录 warn 日志，不传给前端。
     */
    private List<TrafficProduct> filterDirty(List<TrafficProduct> products) {
        List<TrafficProduct> clean = new ArrayList<>();
        for (TrafficProduct p : products) {
            if (p.getArriveTime() != null && p.getDepartTime() != null
                    && !p.getArriveTime().isAfter(p.getDepartTime())) {
                log.warn("[TRAFFIC] dirty data filtered: id={} type={} startPlace={} endPlace={} departTime={} arriveTime={}",
                        p.getId(), p.getType(), p.getStartPlace(), p.getEndPlace(),
                        p.getDepartTime(), p.getArriveTime());
                continue;
            }
            clean.add(p);
        }
        return clean;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
