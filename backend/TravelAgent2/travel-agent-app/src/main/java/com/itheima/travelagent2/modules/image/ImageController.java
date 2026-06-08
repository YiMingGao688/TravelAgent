package com.itheima.travelagent2.modules.image;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.image.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/chat")
    public Result<String> chat(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "model", required = false) String model
    ) throws Exception {

        System.out.println("====== 进入 /api/image/chat 接口 ======");
        System.out.println("file是否为空：" + (file == null || file.isEmpty()));
        System.out.println("text：" + text);

        String result = imageService.chatWithImage(file, text, userId, sessionId, model);

        System.out.println("返回前端结果：" + result);

        return Result.success(result);
    }
}