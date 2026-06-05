package com.itheima.travelagent2.modules.timeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_timeline_item")
public class TravelTimelineItem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String userId;
    private Long tripPlanId;
    private String stage;
    private String serviceType;
    private String title;
    private String content;
    private String triggerTime;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
