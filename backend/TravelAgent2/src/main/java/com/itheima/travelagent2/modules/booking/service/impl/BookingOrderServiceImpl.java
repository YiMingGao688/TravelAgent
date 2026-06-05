package com.itheima.travelagent2.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.booking.dto.OrderCreateDTO;
import com.itheima.travelagent2.modules.booking.entity.BookingOrder;
import com.itheima.travelagent2.modules.booking.mapper.BookingOrderMapper;
import com.itheima.travelagent2.modules.booking.service.BookingOrderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BookingOrderServiceImpl extends ServiceImpl<BookingOrderMapper, BookingOrder> implements BookingOrderService {
    @Override
    public BookingOrder createOrder(OrderCreateDTO dto) {
        BookingOrder order = new BookingOrder();
        order.setUserId(hasText(dto.getUserId()) ? dto.getUserId() : "test_user");
        order.setOrderNo("QWZX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        order.setOrderType(dto.getOrderType());
        order.setProductId(dto.getProductId());
        order.setProductName(dto.getProductName());
        order.setStartPlace(dto.getStartPlace());
        order.setEndPlace(dto.getEndPlace());
        order.setStartTime(dto.getStartTime());
        order.setEndTime(dto.getEndTime());
        order.setTotalAmount(dto.getTotalAmount());
        order.setContactName(hasText(dto.getContactName()) ? dto.getContactName() : "体验用户");
        order.setContactPhone(hasText(dto.getContactPhone()) ? dto.getContactPhone() : "13800000000");
        order.setStatus("UNPAID");
        order.setRemark(dto.getRemark());
        save(order);
        return order;
    }

    @Override
    public List<BookingOrder> listByUserId(String userId) {
        LambdaQueryWrapper<BookingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookingOrder::getUserId, hasText(userId) ? userId : "test_user")
                .orderByDesc(BookingOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<BookingOrder> listAll() {
        return list(new LambdaQueryWrapper<BookingOrder>().orderByDesc(BookingOrder::getCreateTime));
    }

    @Override
    public BookingOrder pay(Long id) {
        BookingOrder order = getById(id);
        if (order == null) {
            return null;
        }
        order.setStatus("PAID");
        updateById(order);
        return getById(id);
    }

    @Override
    public BookingOrder cancel(Long id) {
        BookingOrder order = getById(id);
        if (order == null) {
            return null;
        }
        order.setStatus("CANCELLED");
        updateById(order);
        return getById(id);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
