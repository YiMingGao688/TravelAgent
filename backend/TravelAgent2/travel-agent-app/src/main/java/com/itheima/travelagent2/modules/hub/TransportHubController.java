package com.itheima.travelagent2.modules.hub;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;
import com.itheima.travelagent2.modules.hub.service.TransportHubService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hubs")
public class TransportHubController {
    private final TransportHubService transportHubService;

    public TransportHubController(TransportHubService transportHubService) {
        this.transportHubService = transportHubService;
    }

    @GetMapping("/by-city")
    public Result<List<TransportHub>> byCity(@RequestParam String city) {
        return Result.success(transportHubService.listByCity(city));
    }

    @GetMapping("/recommend")
    public Result<Map<String, TransportHub>> recommend(@RequestParam String startCity,
                                                       @RequestParam String endCity,
                                                       @RequestParam(defaultValue = "TRAIN") String mode) {
        Map<String, TransportHub> result = new LinkedHashMap<>();
        result.put("startHub", transportHubService.recommend(startCity, endCity, mode, true));
        result.put("endHub", transportHubService.recommend(startCity, endCity, mode, false));
        return Result.success(result);
    }
}
