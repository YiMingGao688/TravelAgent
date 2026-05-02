package com.itheima.travelagent2.modules.route.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("route")
public class Route {
    @TableId
    private Long id;

    private String userId;
    private Long tripPlanId;
    private String startPoint;
    private String endPoint;
    private String routeType;
    private String routeName;
    private Double distance;
    private Integer duration;
    private Double cost;
    private String description;
    private String recommendReason;
    private String preference;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
