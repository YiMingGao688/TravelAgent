package com.itheima.travelagent2.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.HotelRoom;
import com.itheima.travelagent2.modules.booking.mapper.HotelMapper;
import com.itheima.travelagent2.modules.booking.mapper.HotelRoomMapper;
import com.itheima.travelagent2.modules.booking.service.HotelService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {
    private final HotelRoomMapper roomMapper;

    public HotelServiceImpl(HotelRoomMapper roomMapper) {
        this.roomMapper = roomMapper;
    }

    @Override
    public List<Hotel> search(String city, String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();
        if (hasText(city)) {
            wrapper.like(Hotel::getCity, city).or().like(Hotel::getAddress, city);
        }
        if (hasText(keyword)) {
            wrapper.and(w -> w.like(Hotel::getName, keyword)
                    .or().like(Hotel::getAddress, keyword)
                    .or().like(Hotel::getTags, keyword)
                    .or().like(Hotel::getCity, keyword));
        }
        if (minPrice != null) {
            wrapper.ge(Hotel::getMinPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Hotel::getMinPrice, maxPrice);
        }
        wrapper.orderByDesc(Hotel::getScore).orderByAsc(Hotel::getMinPrice);
        return list(wrapper);
    }

    @Override
    public HotelDetailDTO detail(Long id) {
        Hotel hotel = getById(id);
        if (hotel == null) {
            return null;
        }
        LambdaQueryWrapper<HotelRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(HotelRoom::getHotelId, id).orderByAsc(HotelRoom::getPrice);
        HotelDetailDTO detail = new HotelDetailDTO();
        detail.setHotel(hotel);
        detail.setRooms(roomMapper.selectList(roomWrapper));
        return detail;
    }

    @Override
    public List<Hotel> listAll() {
        return list(new LambdaQueryWrapper<Hotel>().orderByDesc(Hotel::getCreateTime));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
