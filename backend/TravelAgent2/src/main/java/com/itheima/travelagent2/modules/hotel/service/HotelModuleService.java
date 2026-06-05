package com.itheima.travelagent2.modules.hotel.service;

import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelOrderCreateRequest;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.dto.LocationTipDTO;
import com.itheima.travelagent2.modules.hotel.entity.HotelOrder;

import java.util.List;

public interface HotelModuleService {
    List<HotelItemDTO> searchHotels(HotelSearchRequest request);

    HotelDetailDTO detail(Long id);

    HotelOrder createOrder(HotelOrderCreateRequest request);

    List<LocationTipDTO> locationTips(String keyword);

    List<LocationTipDTO> locationTips(String keyword, String city);
}
