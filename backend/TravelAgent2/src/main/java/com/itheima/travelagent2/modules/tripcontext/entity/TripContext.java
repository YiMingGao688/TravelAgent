package com.itheima.travelagent2.modules.tripcontext.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_context")
public class TripContext {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String sessionId;
    private String userId;
    private String startPlace;
    private String endPlace;
    private String sceneType;
    private Integer days;
    private String preference;
    private String budget;
    private String companionType;
    private String transportMode;
    private String pacePreference;
    private String walkingPreference;
    private String hotelPreference;
    private String foodPreference;
    private String specialNeeds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
