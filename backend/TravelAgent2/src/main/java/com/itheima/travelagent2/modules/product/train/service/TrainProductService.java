package com.itheima.travelagent2.modules.product.train.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;

import java.util.List;

public interface TrainProductService extends IService<TrainProduct> {
    List<TrainProduct> search(String startStation, String endStation, String date);
}
