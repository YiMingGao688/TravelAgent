package com.itheima.travelagent2.modules.booking.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交通产品模拟数据。
 * 覆盖高铁、飞机、公交、地铁、打车等类型，用于首页交通入口形成“搜索-下单”闭环。
 */
@Data
@TableName("traffic_product")
public class TrafficProduct {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String type;
    private String startPlace;
    private String endPlace;
    private LocalDateTime departTime;
    private LocalDateTime arriveTime;
    private String duration;
    private BigDecimal price;
    private String carrierName;
    private String seatType;
    private Integer remainCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
