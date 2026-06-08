package com.itheima.travelagent2.modules.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trip_plan")
/**
 * 历史方案实体。
 * 保存 AI 咨询、行程定制等模块形成的最终方案；结构化字段用于后续导入交通规划和生成服务报告。
 */
public class TripPlan {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String userId;
    private String sessionId;
    private String title;
    /** 出发地，保存前应来自清洗后的 TripContext。 */
    private String startPlace;
    /** 目的地，保存前应来自清洗后的 TripContext。 */
    private String endPlace;
    private String content;
    private String rawQuestion;
    private String sourceType;
    /** 用户偏好，例如时间优先、美食、散步、少走路等。 */
    private String travelPreference;
    /** 用户预算，保留字符串是为了兼容“3000以内/低预算”等自然语言表达。 */
    private String budget;
    /** 出行天数，供行程定制和历史列表展示使用。 */
    private Integer days;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
