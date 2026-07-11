package com.itheima.travelagent2.modules.agentplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_step_record")
public class AgentStepRecord {
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String agentName;
    private String stepKey;
    private String stepTitle;
    private String stepContent;
    private String status;
    private String inputJson;
    private String outputJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
