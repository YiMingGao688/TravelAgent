package com.itheima.travelagent2.modules.booking.dto;

import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.HotelRoom;
import lombok.Data;

import java.util.List;

/**
 * 酒店详情返回对象：酒店基础信息 + 房型列表。
 */
@Data
public class HotelDetailDTO {
    private Hotel hotel;
    private List<HotelRoom> rooms;
}
