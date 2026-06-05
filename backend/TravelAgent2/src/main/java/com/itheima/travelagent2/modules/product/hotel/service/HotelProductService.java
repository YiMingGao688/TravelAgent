package com.itheima.travelagent2.modules.product.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;

import java.math.BigDecimal;
import java.util.List;

public interface HotelProductService extends IService<HotelProduct> {
    List<HotelProduct> search(String city, String nearPlace, BigDecimal lng, BigDecimal lat, Integer radius);
}
