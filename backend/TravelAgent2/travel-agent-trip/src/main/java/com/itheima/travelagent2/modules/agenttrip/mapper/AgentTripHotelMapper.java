package com.itheima.travelagent2.modules.agenttrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentTripHotelMapper extends BaseMapper<Hotel> {
}
