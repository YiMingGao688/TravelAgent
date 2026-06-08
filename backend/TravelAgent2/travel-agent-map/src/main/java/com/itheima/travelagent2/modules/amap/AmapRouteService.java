package com.itheima.travelagent2.modules.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.amap.dto.AmapRouteResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AmapRouteService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${amap.key:${integration.amap-key:}}")
    private String amapKey;

    public AmapRouteService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public AmapRouteResult planDriving(String startLngLat, String endLngLat) {
        return planDirection("DRIVE", "https://restapi.amap.com/v3/direction/driving", startLngLat, endLngLat);
    }

    public AmapRouteResult planWalking(String startLngLat, String endLngLat) {
        return planDirection("WALK", "https://restapi.amap.com/v3/direction/walking", startLngLat, endLngLat);
    }

    public AmapRouteResult planRiding(String startLngLat, String endLngLat) {
        return planDirection("RIDE", "https://restapi.amap.com/v4/direction/bicycling", startLngLat, endLngLat);
    }

    private AmapRouteResult planDirection(String mode, String endpoint, String startLngLat, String endLngLat) {
        requireLngLat(startLngLat, "start");
        requireLngLat(endLngLat, "end");
        try {
            AmapGeoService.acquireRateLimit();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint)
                    .queryParam("key", amapKey)
                    .queryParam(mode.equals("RIDE") ? "origin" : "origin", startLngLat)
                    .queryParam(mode.equals("RIDE") ? "destination" : "destination", endLngLat)
                    .queryParam("output", "JSON");
            if (!mode.equals("RIDE")) {
                builder.queryParam("extensions", "base");
            }
            String response = restTemplate.getForObject(builder.build(false).toUriString(), String.class);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException(root.path("info").asText("AMap route failed")
                        + ", infocode=" + root.path("infocode").asText()
                        + ", response=" + response);
            }
            JsonNode path = firstPath(root, mode);
            double distanceKm = path.path("distance").asDouble(0D) / 1000D;
            int durationMinutes = Math.max(1, (int) Math.ceil(path.path("duration").asDouble(0D) / 60D));
            double cost = mode.equals("DRIVE") ? path.path("tolls").asDouble(0D) : 0D;
            AmapRouteResult result = new AmapRouteResult();
            result.setMode(mode);
            result.setStartLocation(startLngLat);
            result.setEndLocation(endLngLat);
            result.setDistanceKm(round(distanceKm));
            result.setDurationMinutes(durationMinutes);
            result.setEstimatedCost(round(cost));
            result.setRouteSummary("高德路线约 " + result.getDistanceKm() + " 公里，预计 " + durationMinutes + " 分钟");
            result.setRawPolyline(extractPolyline(path));
            System.out.println("[AMAP] route " + mode.toLowerCase()
                    + " duration=" + durationMinutes + "min"
                    + " distance=" + result.getDistanceKm() + "km");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("AMap route " + mode + " failed: " + e.getMessage(), e);
        }
    }

    private JsonNode firstPath(JsonNode root, String mode) {
        JsonNode paths = mode.equals("RIDE") ? root.path("data").path("paths") : root.path("route").path("paths");
        if (!paths.isArray() || paths.isEmpty()) {
            throw new IllegalStateException("AMap route returned empty paths");
        }
        return paths.get(0);
    }

    private String extractPolyline(JsonNode path) {
        StringBuilder builder = new StringBuilder();
        JsonNode steps = path.path("steps");
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                String polyline = step.path("polyline").asText("");
                if (!polyline.isBlank()) {
                    if (!builder.isEmpty()) builder.append(";");
                    builder.append(polyline);
                }
            }
        }
        return builder.toString();
    }

    private void requireLngLat(String value, String field) {
        if (value == null || !value.contains(",")) {
            throw new IllegalArgumentException("不允许没有经纬度就规划路线：" + field);
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("经纬度格式不合法：" + field);
        }
        Double.parseDouble(parts[0]);
        Double.parseDouble(parts[1]);
    }

    private double round(double value) {
        return Math.round(value * 10D) / 10D;
    }
}
