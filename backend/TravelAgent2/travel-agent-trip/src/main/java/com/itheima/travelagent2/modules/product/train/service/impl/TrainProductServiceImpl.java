package com.itheima.travelagent2.modules.product.train.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.product.train.mapper.TrainProductMapper;
import com.itheima.travelagent2.modules.product.train.service.TrainProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrainProductServiceImpl extends ServiceImpl<TrainProductMapper, TrainProduct> implements TrainProductService {

    private static final Logger log = LoggerFactory.getLogger(TrainProductServiceImpl.class);

    @Override
    public List<TrainProduct> search(String startStation, String endStation, String date) {
        LambdaQueryWrapper<TrainProduct> wrapper = new LambdaQueryWrapper<>();
        if (hasText(startStation)) {
            wrapper.like(TrainProduct::getStartStation, normalizeStation(startStation));
        }
        if (hasText(endStation)) {
            wrapper.like(TrainProduct::getEndStation, normalizeStation(endStation));
        }
        if (hasText(date)) {
            LocalDate localDate = LocalDate.parse(date);
            wrapper.between(TrainProduct::getDepartureTime, localDate.atStartOfDay(), localDate.atTime(LocalTime.MAX));
        }
        wrapper.eq(TrainProduct::getStatus, "AVAILABLE")
                .gt(TrainProduct::getStock, 0)
                .orderByAsc(TrainProduct::getDepartureTime)
                .orderByAsc(TrainProduct::getPrice);
        return filterDirty(list(wrapper));
    }

    /**
     * 过滤脏数据：arrival_time 必须大于 departure_time。
     * 发现脏数据时记录 warn 日志，不传给前端。
     */
    private List<TrainProduct> filterDirty(List<TrainProduct> products) {
        List<TrainProduct> clean = new ArrayList<>();
        for (TrainProduct p : products) {
            if (p.getArrivalTime() != null && p.getDepartureTime() != null
                    && !p.getArrivalTime().isAfter(p.getDepartureTime())) {
                log.warn("[TRAIN] dirty data filtered: id={} trainNo={} startStation={} endStation={} departureTime={} arrivalTime={}",
                        p.getId(), p.getTrainNo(), p.getStartStation(), p.getEndStation(),
                        p.getDepartureTime(), p.getArrivalTime());
                continue;
            }
            clean.add(p);
        }
        return clean;
    }

    private String normalizeStation(String station) {
        String value = station == null ? "" : station.trim();
        if (!value.endsWith("站")) {
            value += "站";
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
