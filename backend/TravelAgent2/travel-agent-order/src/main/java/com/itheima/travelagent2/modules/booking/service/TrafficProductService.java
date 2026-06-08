package com.itheima.travelagent2.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;

import java.util.List;

public interface TrafficProductService extends IService<TrafficProduct> {
    List<TrafficProduct> search(String type, String startPlace, String endPlace, String departDate);

    List<TrafficProduct> listAll();
}
