package com.itheima.travelagent2.modules.chat.service;

import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.chat.dto.ChatStreamEvent;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    List<ChatRecord> list();

    List<ChatRecord> listBySessionId(String sessionId);

    Flux<String> streamChat(ChatRequestDTO dto);

    Flux<ChatStreamEvent> streamChatEvents(ChatRequestDTO dto);

    void saveProfile(String userId, ChatRequestDTO dto);

    UserProfile getProfile(String userId);
}
