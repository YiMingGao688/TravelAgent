package com.itheima.travelagent2.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.travelagent2.modules.booking.dto.OrderCreateDTO;
import com.itheima.travelagent2.modules.booking.entity.BookingOrder;

import java.util.List;

public interface BookingOrderService extends IService<BookingOrder> {
    BookingOrder createOrder(OrderCreateDTO dto);

    List<BookingOrder> listByUserId(String userId);

    List<BookingOrder> listAll();

    BookingOrder pay(Long id);

    BookingOrder cancel(Long id);
}
