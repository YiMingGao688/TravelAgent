package com.itheima.travelagent2.modules.tripcontext.service;

import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.tripcontext.dto.TripContextPatchDTO;
import com.itheima.travelagent2.modules.tripcontext.entity.TripContext;

public interface TripContextService {
    TripContext getBySessionId(String sessionId);

    TripContext mergeForMessage(ChatRequestDTO dto);

    TripContextPatchDTO parsePatch(String message);
}
