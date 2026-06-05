package com.itheima.travelagent2.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.booking.entity.Hotel;

import java.math.BigDecimal;
import java.util.List;

public interface HotelService extends IService<Hotel> {
    List<Hotel> search(String city, String keyword, BigDecimal minPrice, BigDecimal maxPrice);

    HotelDetailDTO detail(Long id);

    List<Hotel> listAll();
}
