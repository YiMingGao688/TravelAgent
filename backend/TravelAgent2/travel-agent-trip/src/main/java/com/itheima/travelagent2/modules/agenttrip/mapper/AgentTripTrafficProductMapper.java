package com.itheima.travelagent2.modules.agenttrip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentTripTrafficProductMapper extends BaseMapper<TrafficProduct> {
}
