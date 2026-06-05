package com.itheima.travelagent2.modules.hotel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HotelSearchResponse {
    private String source;
    private List<HotelItemDTO> hotels = new ArrayList<>();
}
