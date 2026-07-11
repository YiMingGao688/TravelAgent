package com.itheima.travelagent2.modules.agenttrip.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class DashScopeAgentModelService {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    public DashScopeAgentModelService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                      ObjectMapper objectMapper) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
    }

    public Optional<Map<String, Object>> generateJson(String stage, String instruction, Object input) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return Optional.empty();
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String prompt = """
                        你是智能出行系统中的阶段智能体。只返回一个合法 JSON 对象，不要返回 Markdown，不要输出推理过程。
                        阶段：%s
                        输出必须是可审计的执行摘要和结构化字段，禁止暴露模型原始思维链。
                        %s
                        输入：%s
                        """.formatted(stage, instruction, objectMapper.writeValueAsString(input));
                String content = builder.build()
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();
                return Optional.of(objectMapper.readValue(cleanJson(content), new TypeReference<>() {
                }));
            } catch (Exception ignored) {
                // 重试一次，仍失败则由调用方走规则兜底。
            }
        }
        return Optional.empty();
    }

    private String cleanJson(String content) {
        String text = String.valueOf(content == null ? "" : content).trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = text.lastIndexOf("```");
            if (end >= 0) text = text.substring(0, end);
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
