package com.itheima.travelagent2.service;

import com.itheima.travelagent2.dto.ChatRequestDTO;
import com.itheima.travelagent2.entity.ChatRecord;
import java.util.List;

public interface ChatService {
    ChatRecord chat(ChatRequestDTO dto);
    List<ChatRecord> list();
}