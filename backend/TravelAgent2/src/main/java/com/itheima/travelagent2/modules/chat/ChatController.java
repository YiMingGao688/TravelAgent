package com.itheima.travelagent2.modules.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.chat.dto.ChatRequestDTO;
import com.itheima.travelagent2.modules.chat.dto.ChatStreamEvent;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.chat.service.ChatService;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
/**
 * AI 咨询接口层。
 * 负责接收前端聊天、用户画像和历史记录请求，真正的上下文合并与大模型调用交给 ChatService。
 */
public class ChatController {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(@RequestBody ChatRequestDTO dto) {
        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            return Result.error(400, "Message is required");
        }
        List<ChatStreamEvent> events = chatService.streamChatEvents(dto).collectList().block();
        StringBuilder reply = new StringBuilder();
        Object tripContext = null;
        if (events != null) {
            for (ChatStreamEvent event : events) {
                if ("message".equals(event.getEventName())) {
                    reply.append(event.getData());
                }
                if ("context".equals(event.getEventName())) {
                    tripContext = event.getData();
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("aiResponse", reply.toString());
        data.put("reply", reply.toString());
        data.put("tripContext", tripContext);
        return Result.success(data);
    }

    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    /**
     * 流式聊天接口。
     * 前端通过 SSE 分段接收 context/message/done 事件，用于实现打字机式 AI 回复。
     */
    public SseEmitter streamMessage(@RequestBody ChatRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(0L);

        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            sendAndComplete(emitter, "error", "Message is required");
            return emitter;
        }

        executor.execute(() -> chatService.streamChatEvents(dto).subscribe(
                event -> send(emitter, event.getEventName(), encodeEventData(event.getData())),
                error -> sendAndComplete(emitter, "error", "AI service error: " + error.getMessage()),
                () -> sendAndComplete(emitter, "done", "[DONE]")
        ));

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        return emitter;
    }

    @PostMapping("/profile/save")
    /**
     * 保存用户画像。
     * 用户画像是默认偏好来源，但优先级低于当前会话 TripContext 和用户本轮明确输入。
     */
    public Result<?> saveProfile(@RequestBody ChatRequestDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().trim().isEmpty()) {
            return Result.error(400, "User id is required");
        }
        chatService.saveProfile(dto.getUserId(), dto);
        return Result.success("Saved");
    }

    @GetMapping("/profile/get")
    public Result<UserProfile> getProfile(@RequestParam String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error(400, "User id is required");
        }
        return Result.success(chatService.getProfile(userId));
    }

    @PostMapping("/profile")
    public Result<?> saveProfileOld(@RequestBody ChatRequestDTO dto) {
        return saveProfile(dto);
    }

    @GetMapping("/list")
    public Result<List<ChatRecord>> list() {
        return Result.success(chatService.list());
    }

    @GetMapping("/history")
    /**
     * 查询指定会话历史。
     * 前端重新打开同一 sessionId 时，可用该接口恢复多轮对话记录。
     */
    public Result<List<ChatRecord>> history(@RequestParam String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error(400, "sessionId is required");
        }
        return Result.success(chatService.listBySessionId(sessionId));
    }

    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            emitter.complete();
        }
    }

    private void sendAndComplete(SseEmitter emitter, String eventName, String data) {
        send(emitter, eventName, data);
        emitter.complete();
    }

    private String encodeEventData(Object data) {
        if (data == null || data instanceof String) {
            return data == null ? "" : String.valueOf(data);
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
