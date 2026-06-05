package com.itheima.travelagent2.modules.hotel.controller;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelOrderCreateRequest;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.entity.HotelOrder;
import com.itheima.travelagent2.modules.hotel.service.HotelModuleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final HotelModuleService hotelModuleService;

    public HotelController(HotelModuleService hotelModuleService) {
        this.hotelModuleService = hotelModuleService;
    }

    /**
     * 酒店搜索：当前主路径为高德 POI 酒店地点搜索，返回真实酒店名称、地址、经纬度；
     * 价格、评分、图片由本地演示规则补充。高德失败时自动降级本地酒店表。
     */
    @GetMapping("/search")
    public Result<List<HotelItemDTO>> search(@RequestParam(required = false) String cityName,
                                             @RequestParam(required = false) String adcode,
                                             @RequestParam(required = false) BigDecimal latitude,
                                             @RequestParam(required = false) BigDecimal longitude,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                                             @RequestParam(required = false) Integer adults,
                                             @RequestParam(required = false) Integer children,
                                             @RequestParam(required = false) Integer rooms,
                                             @RequestParam(required = false) String preference,
                                             @RequestParam(required = false) BigDecimal minPrice,
                                             @RequestParam(required = false) BigDecimal maxPrice,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(required = false) String keyword) {
        HotelSearchRequest request = new HotelSearchRequest();
        request.setCityName(firstText(cityName, city, keyword));
        request.setAdcode(adcode);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setAdults(adults);
        request.setChildren(children);
        request.setRooms(rooms);
        request.setPreference(preference);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        return Result.success("success", hotelModuleService.searchHotels(request));
    }

    @GetMapping("/{id}")
    public Result<HotelDetailDTO> detail(@PathVariable Long id) {
        HotelDetailDTO detail = hotelModuleService.detail(id);
        if (detail == null) {
            return Result.error(404, "酒店不存在");
        }
        return Result.success("success", detail);
    }

    @PostMapping("/orders")
    public Result<HotelOrder> createOrder(@RequestBody HotelOrderCreateRequest request) {
        return Result.success("created", hotelModuleService.createOrder(request));
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
