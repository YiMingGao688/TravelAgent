package com.itheima.travelagent2.modules.hotel.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HotelOrderCreateRequest {
    private String userId;
    private String hotelId;
    private String hotelName;
    private String city;
    private String address;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer adults;
    private Integer children;
    private Integer rooms;
    private String roomType;
    private BigDecimal price;
}
