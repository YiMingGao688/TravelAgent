package com.itheima.travelagent2.modules.hotel.service;

import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;

import java.util.List;

/**
 * 第三方酒店供应商适配层。
 * 当前按 Amadeus 的认证和搜索模式预留，后续切 Expedia/RapidAPI 时只需要替换实现。
 */
public interface HotelApiClient {
    List<HotelItemDTO> search(HotelSearchRequest request);
}
