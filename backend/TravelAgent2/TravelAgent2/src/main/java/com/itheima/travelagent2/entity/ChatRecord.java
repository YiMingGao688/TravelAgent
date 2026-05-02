package com.itheima.travelagent2.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("chat_record")
public class ChatRecord {

    private Long id;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String model;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}