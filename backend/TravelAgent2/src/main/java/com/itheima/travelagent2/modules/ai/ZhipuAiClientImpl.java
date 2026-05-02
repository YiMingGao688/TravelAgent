package com.itheima.travelagent2.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.config.ZhipuAiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class ZhipuAiClientImpl implements ZhipuAiClient {

    private final RestTemplate restTemplate;
    private final ZhipuAiProperties properties;
    private final ObjectMapper objectMapper;

    public ZhipuAiClientImpl(RestTemplate restTemplate, ZhipuAiProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userPrompt) {
        ensureConfigured();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt, false);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getBaseUrl(),
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Zhipu API returned empty body");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Zhipu API returned no choices: " + responseBody);
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null || message.get("content") == null) {
            throw new IllegalStateException("Zhipu API returned no message content: " + responseBody);
        }

        return String.valueOf(message.get("content"));
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        ensureConfigured();

        HttpURLConnection connection = null;
        try {
            URL url = new URL(properties.getBaseUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getConnectTimeoutMs());
            connection.setReadTimeout(properties.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsBytes(buildRequestBody(systemPrompt, userPrompt, true)));
            }

            int status = connection.getResponseCode();
            InputStream inputStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Zhipu API request failed: " + readAll(inputStream));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isBlank()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String chunk = extractChunk(data);
                    if (chunk != null && !chunk.isEmpty()) {
                        onChunk.accept(chunk);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Zhipu streaming failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt, boolean stream) {
        return Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "stream", stream,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
    }

    private String extractChunk(String data) throws Exception {
        JsonNode root = objectMapper.readTree(data);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode delta = choices.get(0).path("delta");
        if (delta.has("content")) {
            return delta.path("content").asText("");
        }
        JsonNode message = choices.get(0).path("message");
        if (message.has("content")) {
            return message.path("content").asText("");
        }
        return "";
    }

    private String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private void ensureConfigured() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getApiKey().contains("please-fill")) {
            throw new IllegalStateException("Cloud AI service is not configured");
        }
    }
}
