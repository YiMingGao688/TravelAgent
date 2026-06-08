package com.itheima.travelagent2.modules.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.route.dto.WeatherInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AmapWeatherService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${integration.weather-key:${amap.key:${integration.amap-key:}}}")
    private String weatherKey;

    public AmapWeatherService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public WeatherInfo getLiveWeather(String adcode) {
        return query(adcode, "base");
    }

    public WeatherInfo getForecastWeather(String adcode) {
        return query(adcode, "all");
    }

    private WeatherInfo query(String adcode, String extensions) {
        if (adcode == null || adcode.isBlank()) {
            throw new IllegalArgumentException("天气查询必须提供 adcode");
        }
        try {
            AmapGeoService.acquireRateLimit();
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/weather/weatherInfo")
                    .queryParam("key", weatherKey)
                    .queryParam("city", adcode)
                    .queryParam("extensions", extensions)
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException("AMap weather failed, info=" + root.path("info").asText());
            }
            JsonNode live = root.path("lives").isArray() && !root.path("lives").isEmpty()
                    ? root.path("lives").get(0)
                    : null;
            if (live == null) {
                return null;
            }
            WeatherInfo weather = new WeatherInfo();
            weather.setProvince(text(live, "province"));
            weather.setCity(text(live, "city"));
            weather.setAdcode(text(live, "adcode"));
            weather.setWeather(text(live, "weather"));
            weather.setTemperature(text(live, "temperature"));
            weather.setWinddirection(text(live, "winddirection"));
            weather.setWindpower(text(live, "windpower"));
            weather.setHumidity(text(live, "humidity"));
            weather.setReporttime(text(live, "reporttime"));
            System.out.println("[AMAP] weather adcode=" + adcode
                    + " weather=" + weather.getWeather()
                    + " temp=" + weather.getTemperature() + "℃");
            return weather;
        } catch (Exception e) {
            throw new RuntimeException("AMap weather failed: " + e.getMessage(), e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }
}
