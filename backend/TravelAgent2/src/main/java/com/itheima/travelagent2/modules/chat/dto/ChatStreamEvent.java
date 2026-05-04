package com.itheima.travelagent2.modules.chat.dto;

import lombok.Data;

@Data
public class ChatStreamEvent {
    private String eventName;
    private Object data;

    public ChatStreamEvent() {
    }

    public ChatStreamEvent(String eventName, Object data) {
        this.eventName = eventName;
        this.data = data;
    }

    public static ChatStreamEvent message(String chunk) {
        return new ChatStreamEvent("message", chunk);
    }

    public static ChatStreamEvent context(Object context) {
        return new ChatStreamEvent("context", context);
    }
}
