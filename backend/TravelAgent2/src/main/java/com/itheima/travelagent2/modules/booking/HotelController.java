package com.itheima.travelagent2.modules.booking;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.service.HotelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页住宿搜索接口。
 */
@RestController
@RequestMapping("/api/booking-hotels")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/search")
    public Result<List<Hotel>> search(@RequestParam(required = false) String city,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String checkInDate,
                                      @RequestParam(required = false) String checkOutDate,
                                      @RequestParam(required = false) BigDecimal minPrice,
                                      @RequestParam(required = false) BigDecimal maxPrice) {
        return Result.success("success", hotelService.search(city, keyword, minPrice, maxPrice));
    }

    @GetMapping("/{id}")
    public Result<HotelDetailDTO> detail(@PathVariable Long id) {
        HotelDetailDTO detail = hotelService.detail(id);
        if (detail == null) {
            return Result.error(404, "酒店不存在");
        }
        return Result.success("success", detail);
    }
}
