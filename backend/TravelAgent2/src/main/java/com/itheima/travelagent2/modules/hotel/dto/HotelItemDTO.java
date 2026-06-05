package com.itheima.travelagent2.modules.hotel.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 酒店搜索结果。保留 name/coverImage/minPrice 等兼容字段，方便复用现有前端酒店卡片。
 */
@Data
public class HotelItemDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String hotelId;
    private String hotelName;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String coverImage;
    private Integer starLevel;
    private BigDecimal rating;
    private BigDecimal score;
    private Integer reviewCount;
    private BigDecimal lowestPrice;
    private BigDecimal minPrice;
    private List<String> facilities;
    private String tags;
    private String description;
    private String distanceText;
    private String source;
}
