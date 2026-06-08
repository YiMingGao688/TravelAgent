package com.itheima.travelagent2.modules.route.weather.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.route.weather.dto.WeatherDTO;
import com.itheima.travelagent2.modules.route.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Value("${amap.key}")
    private String amapKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public WeatherDTO getWeather(String city) {
        if (city == null || city.isBlank()) {
            throw new RuntimeException("城市不能为空");
        }

        String queryCity = city.trim();
        System.out.println("前端传入城市：" + queryCity);

        // 1. 直辖市特殊处理
        if (queryCity.contains("北京")) return queryWeather("110000");
        if (queryCity.contains("天津")) return queryWeather("120000");
        if (queryCity.contains("上海")) return queryWeather("310000");
        if (queryCity.contains("重庆")) return queryWeather("500000");

        // 2. 普通城市先直接查：南京市、马鞍山市
        WeatherDTO direct = queryWeather(queryCity);
        if (direct != null) {
            return direct;
        }

        // 3. 普通城市去掉“市”再查：南京、马鞍山
        String simpleCity = queryCity.replace("市", "");
        WeatherDTO simple = queryWeather(simpleCity);
        if (simple != null) {
            return simple;
        }

        // 4. 还查不到，再走行政区划接口查 adcode
        String adcode = queryAdcode(queryCity);
        System.out.println("查询到的 adcode：" + adcode);

        if (adcode != null && !adcode.isBlank()) {
            WeatherDTO byAdcode = queryWeather(adcode);
            if (byAdcode != null) {
                return byAdcode;
            }
        }

        throw new RuntimeException("未查询到天气数据");
    }
    private WeatherDTO queryWeather(String cityOrAdcode) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/weather/weatherInfo")
                    .queryParam("key", amapKey)
                    .queryParam("city", cityOrAdcode)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            System.out.println("天气请求URL：" + url);

            String response = restTemplate.getForObject(url, String.class);
            System.out.println("天气接口请求参数：" + cityOrAdcode);
            System.out.println("天气接口返回：" + response);

            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.path("status").asText())) {
                System.out.println("天气接口失败原因：" + root.path("info").asText());
                return null;
            }

            JsonNode lives = root.path("lives");
            if (!lives.isArray() || lives.isEmpty()) {
                System.out.println("天气 lives 为空");
                return null;
            }

            JsonNode live = lives.get(0);

            WeatherDTO dto = new WeatherDTO();
            dto.setProvince(live.path("province").asText());
            dto.setCity(live.path("city").asText());
            dto.setAdcode(live.path("adcode").asText());
            dto.setWeather(live.path("weather").asText());
            dto.setTemperature(live.path("temperature").asText());
            dto.setWinddirection(live.path("winddirection").asText());
            dto.setWindpower(live.path("windpower").asText());
            dto.setHumidity(live.path("humidity").asText());
            dto.setReporttime(live.path("reporttime").asText());

            return dto;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String queryAdcode(String city) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/config/district")
                    .queryParam("key", amapKey)
                    .queryParam("keywords", city)
                    .queryParam("subdistrict", 0)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            System.out.println("行政区划请求URL：" + url);

            String response = restTemplate.getForObject(url, String.class);
            System.out.println("行政区划查询参数：" + city);
            System.out.println("行政区划返回：" + response);

            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.path("status").asText())) {
                System.out.println("行政区划失败原因：" + root.path("info").asText());
                return null;
            }

            JsonNode districts = root.path("districts");
            if (!districts.isArray() || districts.isEmpty()) {
                return null;
            }

            return districts.get(0).path("adcode").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}