package com.itheima.travelagent2.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_record")
public class ChatRecord {
    @TableId
    private Long id;

    private String userId;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String model;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
