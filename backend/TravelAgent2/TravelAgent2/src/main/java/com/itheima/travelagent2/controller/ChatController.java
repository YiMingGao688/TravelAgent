package com.itheima.travelagent2.controller;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.dto.ChatRequestDTO;
import com.itheima.travelagent2.entity.ChatRecord;
import com.itheima.travelagent2.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 发送消息
    @PostMapping("/send")
    public Result<ChatRecord> sendMessage(@RequestBody ChatRequestDTO dto) {
        if (dto.getMessage() == null || dto.getMessage().isEmpty()) {
            return Result.error(400, "消息内容不能为空");
        }
        ChatRecord record = chatService.chat(dto);
        return Result.success("消息发送成功", record);
    }

    // 查询所有聊天记录
    @GetMapping("/list")
    public Result<List<ChatRecord>> list() {
        List<ChatRecord> list = chatService.list();
        return Result.success(list);
    }
}