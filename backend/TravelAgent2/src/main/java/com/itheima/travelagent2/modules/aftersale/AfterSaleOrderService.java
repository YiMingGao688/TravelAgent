package com.itheima.travelagent2.modules.aftersale;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.aftersale.entity.AfterSaleOrder;
import com.itheima.travelagent2.modules.aftersale.mapper.AfterSaleOrderMapper;
import com.itheima.travelagent2.modules.booking.entity.BookingOrder;
import com.itheima.travelagent2.modules.booking.mapper.BookingOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AfterSaleOrderService {
    private final AfterSaleOrderMapper mapper;
    private final BookingOrderMapper bookingOrderMapper;

    public AfterSaleOrderService(AfterSaleOrderMapper mapper, BookingOrderMapper bookingOrderMapper) {
        this.mapper = mapper;
        this.bookingOrderMapper = bookingOrderMapper;
    }

    public AfterSaleOrder create(AfterSaleOrder order) {
        if (order.getStatus() == null || order.getStatus().isBlank()) order.setStatus("PENDING");
        if (order.getTitle() == null || order.getTitle().isBlank()) order.setTitle(readableType(order.getServiceType()) + "售后工单");
        mapper.insert(order);
        syncRefundOrderStatus(order, "REFUNDING");
        return order;
    }

    public AfterSaleOrder updateStatus(Long id, AfterSaleOrder payload) {
        AfterSaleOrder order = mapper.selectById(id);
        if (order == null) return null;
        order.setStatus(payload.getStatus());
        order.setProcessor(payload.getProcessor());
        order.setProcessRemark(payload.getProcessRemark());
        mapper.updateById(order);
        syncRefundOrderStatus(order, "APPROVED".equals(order.getStatus()) ? "REFUNDED" : "REJECTED".equals(order.getStatus()) ? "REFUND_REJECTED" : null);
        return mapper.selectById(id);
    }

    public List<AfterSaleOrder> listByUserId(String userId) {
        return mapper.selectList(new LambdaQueryWrapper<AfterSaleOrder>()
                .eq(AfterSaleOrder::getUserId, userId)
                .orderByDesc(AfterSaleOrder::getCreateTime));
    }

    public List<AfterSaleOrder> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<AfterSaleOrder>().orderByDesc(AfterSaleOrder::getCreateTime));
    }

    private String readableType(String type) {
        if ("CHANGE".equals(type)) return "改签";
        if ("REFUND".equals(type)) return "退订";
        if ("COMPLAINT".equals(type)) return "投诉";
        if ("LUGGAGE".equals(type)) return "行李";
        if ("DELAY".equals(type)) return "延误";
        if ("ORDER_ERROR".equals(type)) return "订单异常";
        return "智能";
    }

    private void syncRefundOrderStatus(AfterSaleOrder afterSaleOrder, String targetStatus) {
        if (targetStatus == null || afterSaleOrder.getOrderId() == null) return;
        if (!"REFUND".equalsIgnoreCase(afterSaleOrder.getServiceType())) return;
        BookingOrder bookingOrder = bookingOrderMapper.selectById(afterSaleOrder.getOrderId());
        if (bookingOrder == null) return;
        bookingOrder.setStatus(targetStatus);
        bookingOrderMapper.updateById(bookingOrder);
    }
}
