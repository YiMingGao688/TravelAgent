package com.itheima.travelagent2.modules.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.chat.mapper.ChatRecordMapper;
import com.itheima.travelagent2.modules.chat.service.ChatService;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = """
            You are a customer-facing intelligent travel assistant.
            Always answer in Simplified Chinese.
            Help the user plan real travel arrangements: origin, destination, timing, transport, transfer, budget,
            reminders, backup plan, after-sales help and service suggestions.
            If the user does not provide an origin, but a resident city/default origin is provided, use it as the origin.
            Do not say the origin is unknown in that case.
            Do not expose technical wording such as API, database, prompt, model or competition.
            Do not fabricate real-time ticket inventory, price, traffic or payment results.
            If real-time information is needed, remind the user to confirm before departure.
            """;

    private final ZhipuAiClient zhipuAiClient;
    private final ChatRecordMapper chatRecordMapper;
    private final UserProfileMapper userProfileMapper;

    public ChatServiceImpl(ZhipuAiClient zhipuAiClient,
                           ChatRecordMapper chatRecordMapper,
                           UserProfileMapper userProfileMapper) {
        this.zhipuAiClient = zhipuAiClient;
        this.chatRecordMapper = chatRecordMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public List<ChatRecord> list() {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        return chatRecordMapper.selectList(wrapper);
    }

    @Override
    public Flux<String> streamChat(ChatRequestDTO dto) {
        return Flux.create(sink -> {
            try {
                String sessionId = getOrCreateSessionId(dto);
                dto.setSessionId(sessionId);

                String prompt = buildPromptWithProfile(dto);
                StringBuilder answer = new StringBuilder();
                zhipuAiClient.streamChat(SYSTEM_PROMPT, prompt, chunk -> {
                    answer.append(chunk);
                    sink.next(chunk);
                });

                ChatRecord record = new ChatRecord();
                record.setUserId(dto.getUserId());
                record.setSessionId(sessionId);
                record.setUserMessage(dto.getMessage());
                record.setAiResponse(answer.toString());
                record.setModel("zhipu-cloud-stream");
                record.setCreateTime(LocalDateTime.now());
                chatRecordMapper.insert(record);

                sink.complete();
            } catch (Exception e) {
                e.printStackTrace();
                sink.next("当前智能出行服务暂时不可用，请稍后再试。");
                sink.complete();
            }
        });
    }

    @Override
    public void saveProfile(String userId, ChatRequestDTO dto) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile existingProfile = userProfileMapper.selectOne(wrapper);

        if (existingProfile != null) {
            existingProfile.setCity(dto.getCity());
            existingProfile.setTransport(dto.getTransport());
            existingProfile.setStyle(dto.getStyle());
            existingProfile.setNote(dto.getNote());
            existingProfile.setUpdatedAt(LocalDateTime.now());
            userProfileMapper.updateById(existingProfile);
        } else {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCity(dto.getCity());
            profile.setTransport(dto.getTransport());
            profile.setStyle(dto.getStyle());
            profile.setNote(dto.getNote());
            profile.setCreatedAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileMapper.insert(profile);
        }
    }

    @Override
    public UserProfile getProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        return userProfileMapper.selectOne(wrapper);
    }

    @Override
    public List<ChatRecord> listBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRecord::getSessionId, sessionId).orderByAsc(ChatRecord::getCreateTime);
        return chatRecordMapper.selectList(wrapper);
    }

    private String buildPromptWithProfile(ChatRequestDTO dto) {
        UserProfile profile = dto.getUserId() == null ? null : getProfile(dto.getUserId());

        String city = choose(dto.getCity(), profile == null ? null : profile.getCity(), "not provided");
        String transport = choose(dto.getTransport(), profile == null ? null : profile.getTransport(), "not provided");
        String style = choose(dto.getStyle(), profile == null ? null : profile.getStyle(), "not provided");
        String note = choose(dto.getNote(), profile == null ? null : profile.getNote(), "none");

        StringBuilder historyPrompt = new StringBuilder();
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRecord::getSessionId, dto.getSessionId())
                .orderByDesc(ChatRecord::getCreateTime)
                .last("LIMIT 6");
        List<ChatRecord> history = chatRecordMapper.selectList(wrapper);
        if (!history.isEmpty()) {
            historyPrompt.append("Recent conversation:\n");
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatRecord record = history.get(i);
                historyPrompt.append("User: ").append(record.getUserMessage()).append("\n");
                historyPrompt.append("Assistant: ").append(record.getAiResponse()).append("\n\n");
            }
        }

        return """
                User travel profile:
                - Resident city / default origin: %s
                - Preferred transport: %s
                - Travel style: %s
                - Notes: %s

                Important rules:
                - If the current question does not explicitly include an origin, use the resident city/default origin.
                - Start the answer by stating the recognized origin, recognized destination and used preference.
                - Answer in Simplified Chinese for an ordinary customer.

                %s
                Current user question:
                %s
                """.formatted(city, transport, style, note, historyPrompt, choose(dto.getMessage(), null, ""));
    }

    private String getOrCreateSessionId(ChatRequestDTO dto) {
        if (dto.getSessionId() != null && !dto.getSessionId().trim().isEmpty()) {
            return dto.getSessionId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private String choose(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return fallback;
    }
}
