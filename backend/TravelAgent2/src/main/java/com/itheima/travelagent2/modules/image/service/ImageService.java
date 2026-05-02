package com.itheima.travelagent2.modules.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    String chatWithImage(
            MultipartFile file,
            String text,
            String userId,
            String sessionId,
            String model
    ) throws Exception;
}