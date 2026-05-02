package com.itheima.travelagent2.service.impl;

import com.itheima.travelagent2.dto.ChatRequestDTO;
import com.itheima.travelagent2.entity.ChatRecord;
import com.itheima.travelagent2.mapper.ChatRecordMapper;
import com.itheima.travelagent2.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatRecordMapper chatRecordMapper;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, ChatRecordMapper chatRecordMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是专业的智慧出行旅游AI助手，专注行程规划、景点推荐、路线查询、旅游攻略。
                        回答条理清晰、内容完整、友好专业。
                        """)
                .build();
        this.chatRecordMapper = chatRecordMapper;
    }

    @Override
    public ChatRecord chat(ChatRequestDTO dto) {
        String sessionId = UUID.randomUUID().toString();
        String userMsg = dto.getMessage();
        String aiReply = "AI服务异常，请检查Ollama是否启动";

        try {
            aiReply = chatClient.prompt()
                    .user(userMsg)
                    .call()
                    .content();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ChatRecord record = new ChatRecord();
        record.setSessionId(sessionId);
        record.setUserMessage(userMsg);
        record.setAiResponse(aiReply);
        record.setCreateTime(LocalDateTime.now());
        chatRecordMapper.insert(record);

        return record;
    }

    @Override
    public List<ChatRecord> list() {
        return chatRecordMapper.selectList(null);
    }
}