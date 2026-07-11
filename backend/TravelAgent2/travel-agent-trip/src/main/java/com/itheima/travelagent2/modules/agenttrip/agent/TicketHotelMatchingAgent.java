package com.itheima.travelagent2.modules.agenttrip.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentTripHotelMapper;
import com.itheima.travelagent2.modules.agenttrip.mapper.AgentTripTrafficProductMapper;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TicketHotelMatchingAgent {
    private final AgentTripTrafficProductMapper trafficProductMapper;
    private final AgentTripHotelMapper hotelMapper;

    public TicketHotelMatchingAgent(AgentTripTrafficProductMapper trafficProductMapper,
                                    AgentTripHotelMapper hotelMapper) {
        this.trafficProductMapper = trafficProductMapper;
        this.hotelMapper = hotelMapper;
    }

    public Map<String, Object> match(Map<String, Object> intent) {
        String startPlace = stringValue(intent.get("startPlace"));
        String endPlace = stringValue(intent.get("endPlace"));
        List<TrafficProduct> traffic = trafficProductMapper.selectList(new LambdaQueryWrapper<TrafficProduct>()
                .and(wrapper -> wrapper
                        .like(!startPlace.isBlank(), TrafficProduct::getStartPlace, startPlace)
                        .or()
                        .like(!endPlace.isBlank(), TrafficProduct::getEndPlace, endPlace)
                        .or()
                        .like(TrafficProduct::getType, "TRAIN")
                        .or()
                        .like(TrafficProduct::getType, "FLIGHT"))
                .last("LIMIT 5"));
        List<Hotel> hotels = hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                .and(wrapper -> wrapper
                        .like(!endPlace.isBlank(), Hotel::getCity, endPlace)
                        .or()
                        .like(!endPlace.isBlank(), Hotel::getAddress, endPlace)
                        .or()
                        .like(!endPlace.isBlank(), Hotel::getName, endPlace))
                .orderByDesc(Hotel::getScore)
                .last("LIMIT 5"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("trafficCandidates", traffic);
        data.put("hotelCandidates", hotels);
        data.put("taxiCandidate", Map.of(
                "title", endPlace + "到酒店接驳",
                "duration", "约25分钟",
                "price", 45,
                "provider", "模拟打车"));
        data.put("summary", "已从本地模拟数据库匹配到" + traffic.size() + "条票务/交通候选和" + hotels.size() + "条酒店候选。");
        return data;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
