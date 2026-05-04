package com.itheima.travelagent2.modules.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.chat.dto.ChatStreamEvent;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.chat.mapper.ChatRecordMapper;
import com.itheima.travelagent2.modules.chat.service.ChatService;
import com.itheima.travelagent2.modules.tripcontext.entity.TripContext;
import com.itheima.travelagent2.modules.tripcontext.service.TripContextService;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {
    private static final int MAX_CONTEXT_RECORDS = 12;
    private static final int MAX_CONTEXT_CHARS = 8000;

    private static final String SYSTEM_PROMPT = """
            你是面向客户的智能出行助手。
            请始终使用简体中文回答。
            你的目标是根据当前会话已确认的出行信息，生成真实、清晰、可执行的出行方案。
            不要暴露 API、数据库、Prompt、模型、比赛等技术词。
            不要编造实时余票、价格、拥堵、支付结果；如果需要实时确认，请提醒用户出发前核验。
            """;

    private final ZhipuAiClient zhipuAiClient;
    private final ChatRecordMapper chatRecordMapper;
    private final UserProfileMapper userProfileMapper;
    private final TripContextService tripContextService;

    public ChatServiceImpl(ZhipuAiClient zhipuAiClient,
                           ChatRecordMapper chatRecordMapper,
                           UserProfileMapper userProfileMapper,
                           TripContextService tripContextService) {
        this.zhipuAiClient = zhipuAiClient;
        this.chatRecordMapper = chatRecordMapper;
        this.userProfileMapper = userProfileMapper;
        this.tripContextService = tripContextService;
    }

    @Override
    public List<ChatRecord> list() {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        return chatRecordMapper.selectList(wrapper);
    }

    @Override
    public List<ChatRecord> listBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRecord::getSessionId, sessionId).orderByAsc(ChatRecord::getCreateTime);
        return chatRecordMapper.selectList(wrapper);
    }

    @Override
    public Flux<String> streamChat(ChatRequestDTO dto) {
        return streamChatEvents(dto)
                .filter(event -> "message".equals(event.getEventName()))
                .map(event -> String.valueOf(event.getData()));
    }

    @Override
    public Flux<ChatStreamEvent> streamChatEvents(ChatRequestDTO dto) {
        return Flux.create(sink -> {
            try {
                String sessionId = getOrCreateSessionId(dto);
                dto.setSessionId(sessionId);

                TripContext tripContext = tripContextService.mergeForMessage(dto);
                sink.next(ChatStreamEvent.context(tripContext));

                String prompt = buildPromptWithProfile(dto, tripContext);
                StringBuilder answer = new StringBuilder();
                zhipuAiClient.streamChat(SYSTEM_PROMPT, prompt, chunk -> {
                    answer.append(chunk);
                    sink.next(ChatStreamEvent.message(chunk));
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
                sink.next(ChatStreamEvent.message("当前智能出行服务暂时不可用，请稍后再试。"));
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

    private String buildPromptWithProfile(ChatRequestDTO dto, TripContext tripContext) {
        UserProfile profile = dto.getUserId() == null ? null : getProfile(dto.getUserId());

        String city = choose(dto.getCity(), profile == null ? null : profile.getCity(), "未提供");
        String transport = choose(dto.getTransport(), profile == null ? null : profile.getTransport(), "未提供");
        String style = choose(dto.getStyle(), profile == null ? null : profile.getStyle(), "未提供");
        String note = choose(dto.getNote(), profile == null ? null : profile.getNote(), "无");

        StringBuilder historyPrompt = new StringBuilder();
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatRecord::getSessionId, dto.getSessionId())
                .orderByDesc(ChatRecord::getCreateTime)
                .last("LIMIT " + MAX_CONTEXT_RECORDS);
        List<ChatRecord> history = chatRecordMapper.selectList(wrapper);
        if (!history.isEmpty()) {
            historyPrompt.append("最近对话上下文，仅用于理解表达方式，不允许覆盖当前会话已确认信息：\n");
            int used = 0;
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatRecord record = history.get(i);
                String block = "user: " + shorten(record.getUserMessage(), 500) + "\n"
                        + "assistant: " + shorten(record.getAiResponse(), 900) + "\n\n";
                if (used + block.length() > MAX_CONTEXT_CHARS) {
                    break;
                }
                historyPrompt.append(block);
                used += block.length();
            }
        }

        return """
                用户画像默认信息：
                - 常驻城市/默认起点：%s
                - 默认交通方式：%s
                - 默认偏好：%s
                - 备注：%s

                当前会话已确认的出行信息：
                - 起点：%s
                - 终点：%s
                - 场景：%s
                - 天数：%s
                - 偏好：%s
                - 预算：%s
                - 同行人群：%s
                - 交通方式：%s
                - 行程节奏：%s
                - 步行接受度：%s
                - 住宿偏好：%s
                - 饮食偏好：%s
                - 特殊需求：%s

                用户本轮要求：
                %s

                生成规则：
                - 请严格基于“当前会话已确认的出行信息”重新生成方案。
                - 如果用户本轮只修改了某一个字段，则其他字段保持不变。
                - 不要擅自改变起点、终点、天数、预算等字段。
                - 如果偏好已经更新为“散步”，方案要围绕城市散步路线、轻松步行节奏和沿途休息点展开，不要继续以美食为主。
                - 回答面向普通客户，不要出现 API、数据库、Prompt、模型、比赛等技术词。
                - 用简体中文输出，并先简要说明本次采用的起点、终点、天数、偏好和预算。

                %s
                """.formatted(
                city, transport, style, note,
                display(tripContext.getStartPlace()),
                display(tripContext.getEndPlace()),
                display(tripContext.getSceneType()),
                tripContext.getDays() == null ? "未确认" : tripContext.getDays(),
                display(tripContext.getPreference()),
                display(tripContext.getBudget()),
                display(tripContext.getCompanionType()),
                display(tripContext.getTransportMode()),
                display(tripContext.getPacePreference()),
                display(tripContext.getWalkingPreference()),
                display(tripContext.getHotelPreference()),
                display(tripContext.getFoodPreference()),
                display(tripContext.getSpecialNeeds()),
                choose(dto.getMessage(), null, ""),
                historyPrompt
        );
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

    private String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String display(String value) {
        return value == null || value.trim().isEmpty() ? "未确认" : value.trim();
    }
}
