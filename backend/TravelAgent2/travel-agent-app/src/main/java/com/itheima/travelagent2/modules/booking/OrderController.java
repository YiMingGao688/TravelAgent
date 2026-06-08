package com.itheima.travelagent2.modules.booking;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.booking.dto.OrderCreateDTO;
import com.itheima.travelagent2.modules.booking.entity.BookingOrder;
import com.itheima.travelagent2.modules.booking.service.BookingOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模拟订单接口。
 * 完成“选择产品 -> 创建订单 -> 模拟支付/取消 -> 用户和管理端查询”的闭环。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final BookingOrderService orderService;

    public OrderController(BookingOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Result<BookingOrder> create(@RequestBody OrderCreateDTO dto) {
        if (dto.getOrderType() == null || dto.getOrderType().isBlank()) {
            return Result.error(400, "orderType is required");
        }
        if (dto.getProductName() == null || dto.getProductName().isBlank()) {
            return Result.error(400, "productName is required");
        }
        return Result.success("success", orderService.createOrder(dto));
    }

    @GetMapping("/user/{userId}")
    public Result<List<BookingOrder>> listByUser(@PathVariable String userId) {
        return Result.success("success", orderService.listByUserId(userId));
    }

    @PutMapping("/{id}/pay")
    public Result<BookingOrder> pay(@PathVariable Long id) {
        BookingOrder order = orderService.pay(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success("success", order);
    }

    @PutMapping("/{id}/cancel")
    public Result<BookingOrder> cancel(@PathVariable Long id) {
        BookingOrder order = orderService.cancel(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success("success", order);
    }
}
