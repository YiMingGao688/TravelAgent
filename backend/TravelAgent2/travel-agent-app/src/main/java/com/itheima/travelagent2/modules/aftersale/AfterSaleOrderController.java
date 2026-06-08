package com.itheima.travelagent2.modules.aftersale;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.aftersale.entity.AfterSaleOrder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleOrderController {
    private final AfterSaleOrderService service;

    public AfterSaleOrderController(AfterSaleOrderService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    public Result<AfterSaleOrder> create(@RequestBody AfterSaleOrder order) {
        return Result.success(service.create(order));
    }

    @PutMapping("/orders/{id}/status")
    public Result<AfterSaleOrder> updateStatus(@PathVariable Long id, @RequestBody AfterSaleOrder order) {
        AfterSaleOrder updated = service.updateStatus(id, order);
        return updated == null ? Result.error(404, "售后工单不存在") : Result.success(updated);
    }

    @GetMapping("/orders/user/{userId}")
    public Result<List<AfterSaleOrder>> listByUser(@PathVariable String userId) {
        return Result.success(service.listByUserId(userId));
    }

    @GetMapping("/orders")
    public Result<List<AfterSaleOrder>> listAll() {
        return Result.success(service.listAll());
    }
}
