package com.itheima.travelagent2.modules.booking;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import com.itheima.travelagent2.modules.booking.service.TrafficProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页交通产品搜索接口。
 */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {
    private final TrafficProductService trafficProductService;

    public TrafficController(TrafficProductService trafficProductService) {
        this.trafficProductService = trafficProductService;
    }

    @GetMapping("/search")
    public Result<List<TrafficProduct>> search(@RequestParam(required = false) String type,
                                               @RequestParam(required = false) String startPlace,
                                               @RequestParam(required = false) String endPlace,
                                               @RequestParam(required = false) String departDate) {
        return Result.success("success", trafficProductService.search(type, startPlace, endPlace, departDate));
    }
}
