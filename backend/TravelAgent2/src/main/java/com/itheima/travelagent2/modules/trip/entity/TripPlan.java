package com.itheima.travelagent2.modules.trip.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_plan")
public class TripPlan {
    @TableId
    private Long id;

    private String userId;
    private String sessionId;
    private String title;
    private String startPlace;
    private String endPlace;
    private String content;
    private String rawQuestion;
    private String sourceType;
    private String travelPreference;
    private String budget;
    private Integer days;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
