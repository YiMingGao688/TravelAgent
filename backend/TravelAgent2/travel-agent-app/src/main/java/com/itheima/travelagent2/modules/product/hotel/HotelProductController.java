package com.itheima.travelagent2.modules.product.hotel;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.hotel.service.HotelProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products/hotels")
public class HotelProductController {
    private final HotelProductService hotelProductService;

    public HotelProductController(HotelProductService hotelProductService) {
        this.hotelProductService = hotelProductService;
    }

    @GetMapping("/search")
    public Result<List<HotelProduct>> search(@RequestParam(required = false) String city,
                                             @RequestParam(required = false) String nearPlace,
                                             @RequestParam(required = false) BigDecimal lng,
                                             @RequestParam(required = false) BigDecimal lat,
                                             @RequestParam(required = false) Integer radius) {
        return Result.success(hotelProductService.search(city, nearPlace, lng, lat, radius));
    }
}
