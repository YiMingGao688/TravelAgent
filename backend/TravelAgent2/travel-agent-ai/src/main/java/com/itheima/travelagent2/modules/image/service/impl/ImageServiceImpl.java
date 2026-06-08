package com.itheima.travelagent2.modules.image.service.impl;

import com.itheima.travelagent2.modules.image.service.ImageService;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageServiceImpl implements ImageService {

    private final ZhipuAiClient zhipuAiClient;

    public ImageServiceImpl(ZhipuAiClient zhipuAiClient) {
        this.zhipuAiClient = zhipuAiClient;
    }

    @Override
    public String chatWithImage(MultipartFile file, String text, String userId, String sessionId, String model) throws Exception {
        if (file == null || file.isEmpty()) {
            return "未检测到图片，请先上传图片。";
        }

        String prompt = """
                用户上传了一张出行相关图片，并补充了如下说明：
                %s

                文件名：%s
                文件大小：%s bytes

                请站在出行顾问角度，给出：
                1. 可能的出行场景判断
                2. 需要注意的安全或时间风险
                3. 下一步建议
                4. 如果信息不足，请温和说明需要用户补充什么
                """.formatted(value(text), file.getOriginalFilename(), file.getSize());

        return zhipuAiClient.chat("你是智慧出行图片场景分析助手。", prompt);
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "用户未补充说明" : value.trim();
    }
}
