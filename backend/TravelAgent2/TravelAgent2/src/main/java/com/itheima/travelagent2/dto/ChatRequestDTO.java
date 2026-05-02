package com.itheima.travelagent2.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String sessionId;
    private String message;
    private String model;
}
