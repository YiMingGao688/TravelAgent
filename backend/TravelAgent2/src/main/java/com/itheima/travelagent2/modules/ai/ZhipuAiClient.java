package com.itheima.travelagent2.modules.ai;

import java.util.function.Consumer;

public interface ZhipuAiClient {
    String chat(String systemPrompt, String userPrompt);

    void streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk);
}
