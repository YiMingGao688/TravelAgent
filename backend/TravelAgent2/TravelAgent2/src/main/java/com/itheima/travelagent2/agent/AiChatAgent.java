package com.itheima.travelagent2.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AiChatAgent {

    private final ChatClient chatClient;

    public AiChatAgent(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是智慧出行AI助手，专注旅游规划、路线推荐、攻略生成。
                        回复要结构清晰，分点说明。
                        """)
                .build();
    }

    public String chat(String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}