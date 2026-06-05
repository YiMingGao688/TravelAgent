package com.itheima.travelagent2.modules.hotel.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 酒店搜索请求。
 * 前端只传用户选择后的结构化地点和入住条件，避免把自然语言直接交给酒店 API。
 */
@Data
public class HotelSearchRequest {
    private String cityName;
    private String adcode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer adults;
    private Integer children;
    private Integer rooms;
    private String preference;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
