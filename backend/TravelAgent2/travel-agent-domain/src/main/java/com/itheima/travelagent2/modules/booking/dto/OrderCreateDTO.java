package com.itheima.travelagent2.modules.booking.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建模拟订单请求。
 */
@Data
public class OrderCreateDTO {
    private String userId;
    private String orderType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productName;
    private String startPlace;
    private String endPlace;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private String remark;
}
