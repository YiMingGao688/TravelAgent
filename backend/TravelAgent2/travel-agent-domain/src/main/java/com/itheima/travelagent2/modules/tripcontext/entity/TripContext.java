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
/**
 * 当前会话出行上下文。
 * 它解决多轮咨询“只修改本轮明确字段、其他字段保持不变”的问题，是 AI 咨询、保存方案和导入模块的共同数据源。
 */
public class TripContext {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String sessionId;
    private String userId;
    /** 出发地；单城市旅游表达不应误填为起点。 */
    private String startPlace;
    /** 目的地；入库和返回前会做地点清洗。 */
    private String endPlace;
    /** 出行场景，例如休闲旅行、商务出行、通勤等。 */
    private String sceneType;
    /** 出行天数，支持从“三天/3日游”等自然语言中提取。 */
    private Integer days;
    /** 当前会话明确偏好，优先级高于用户画像默认偏好。 */
    private String preference;
    /** 当前会话预算，允许保存自然语言预算描述。 */
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
