package com.itheima.travelagent2.modules.hotel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 酒店模块本地模拟订单。
 * 第三方酒店 API 只用于搜索和展示，预订先落本地表，避免真实支付和真实库存风险。
 */
@Data
@TableName("hotel_order")
public class HotelOrder {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
