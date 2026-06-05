package com.itheima.travelagent2.modules.hotel.controller;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.amap.AmapGeoService;
import com.itheima.travelagent2.modules.hotel.dto.LocationTipDTO;
import com.itheima.travelagent2.modules.hotel.service.HotelModuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {
    private final HotelModuleService hotelModuleService;

    public LocationController(HotelModuleService hotelModuleService) {
        this.hotelModuleService = hotelModuleService;
    }

    /**
     * 高德地点输入提示。前端不直接持有高德 key，只消费后端返回的受控候选地点。
     * 可选传入 city 限定搜索范围，避免全国乱搜。
     */
    @GetMapping("/tips")
    public Result<List<LocationTipDTO>> tips(@RequestParam String keyword,
                                             @RequestParam(required = false) String city) {
        try {
            return Result.success(hotelModuleService.locationTips(keyword, city == null ? "" : city));
        } catch (AmapGeoService.AmapRateLimitException e) {
            return Result.error(429, e.getMessage());
        }
    }
}
