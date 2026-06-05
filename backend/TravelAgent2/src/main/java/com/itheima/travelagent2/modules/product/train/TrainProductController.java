package com.itheima.travelagent2.modules.product.train;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.product.train.service.TrainProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/trains")
public class TrainProductController {
    private final TrainProductService trainProductService;

    public TrainProductController(TrainProductService trainProductService) {
        this.trainProductService = trainProductService;
    }

    @GetMapping("/search")
    public Result<List<TrainProduct>> search(@RequestParam String startStation,
                                             @RequestParam String endStation,
                                             @RequestParam(required = false) String date) {
        return Result.success(trainProductService.search(startStation, endStation, date));
    }
}
