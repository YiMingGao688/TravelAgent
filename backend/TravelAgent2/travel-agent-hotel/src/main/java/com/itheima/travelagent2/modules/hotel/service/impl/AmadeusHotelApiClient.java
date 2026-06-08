package com.itheima.travelagent2.modules.hotel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.service.HotelApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class AmadeusHotelApiClient implements HotelApiClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hotel.api.provider:amadeus}")
    private String provider;

    @Value("${hotel.api.amadeus.base-url:https://test.api.amadeus.com}")
    private String baseUrl;

    @Value("${hotel.api.amadeus.client-id:}")
    private String clientId;

    @Value("${hotel.api.amadeus.client-secret:}")
    private String clientSecret;

    @Override
    public List<HotelItemDTO> search(HotelSearchRequest request) {
        if (!"amadeus".equalsIgnoreCase(provider) || isBlank(clientId) || isBlank(clientSecret)) {
            return List.of();
        }
        try {
            String token = accessToken();
            if (isBlank(token)) {
                return List.of();
            }
            return searchByGeo(request, token);
        } catch (Exception ex) {
            System.out.println("第三方酒店 API 调用失败，降级本地酒店数据：" + ex.getMessage());
            return List.of();
        }
    }

    private String accessToken() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/v1/security/oauth2/token", new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("access_token").asText("");
    }

    private List<HotelItemDTO> searchByGeo(HotelSearchRequest request, String token) throws Exception {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return List.of();
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v1/reference-data/locations/hotels/by-geocode")
                .queryParam("latitude", request.getLatitude())
                .queryParam("longitude", request.getLongitude())
                .queryParam("radius", 8)
                .queryParam("radiusUnit", "KM")
                .build()
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        List<HotelItemDTO> hotels = new ArrayList<>();
        for (JsonNode item : data) {
            HotelItemDTO hotel = new HotelItemDTO();
            hotel.setHotelId(item.path("hotelId").asText());
            hotel.setName(item.path("name").asText("酒店"));
            hotel.setHotelName(hotel.getName());
            hotel.setAddress(item.path("address").path("lines").isArray() ? item.path("address").path("lines").get(0).asText("") : "");
            hotel.setLatitude(decimal(item.path("geoCode").path("latitude").asText("0")));
            hotel.setLongitude(decimal(item.path("geoCode").path("longitude").asText("0")));
            hotel.setCoverImage("https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80");
            hotel.setStarLevel(4);
            hotel.setRating(new BigDecimal("4.6"));
            hotel.setScore(hotel.getRating());
            hotel.setReviewCount(120);
            hotel.setLowestPrice(new BigDecimal("499"));
            hotel.setMinPrice(hotel.getLowestPrice());
            hotel.setFacilities(List.of("真实API", "可预订演示", "位置已校验"));
            hotel.setTags("真实API,位置已校验,可预订演示");
            hotel.setDescription("来自 Amadeus 酒店接口的酒店位置数据，价格暂按演示价展示。");
            hotel.setDistanceText("目的地附近");
            hotel.setSource("AMADEUS");
            hotels.add(hotel);
        }
        return hotels;
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
