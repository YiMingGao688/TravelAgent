package com.itheima.travelagent2.modules.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.hotel.dto.LocationTipDTO;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AmapGeoService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * ★ 高德 QPS 限流器（全局共享）：滑动窗口，每分钟最多 180 次（≈3 QPS）。
     * 超过限制时抛出 AmapRateLimitException，由调用方捕获后返回友好提示。
     * 声明为 static 以便 AmapRouteService、AmapWeatherService 共用同一个限流器。
     */
    private static final int MAX_CALLS_PER_MINUTE = 180;
    private static final LinkedList<Long> recentCallTimestamps = new LinkedList<>();

    @Value("${amap.key:${integration.amap-key:}}")
    private String amapKey;

    /**
     * inputTips 缓存：key = keyword + "|" + city，30 秒过期。
     * value 为 null 表示上次请求失败，缓存期内不再重试。
     */
    private final ConcurrentHashMap<String, CacheEntry<List<LocationTipDTO>>> tipCache = new ConcurrentHashMap<>();

    public AmapGeoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 高德地点输入提示（无城市限定）。兼容旧调用方，内部使用空字符串作为城市。
     */
    public List<LocationTipDTO> inputTips(String keyword) {
        return inputTips(keyword, "");
    }

    /**
     * 高德地点输入提示，限定城市/adcode，避免全国乱搜。
     *
     * @param keyword 搜索关键词，少于 2 个字符不请求
     * @param city    城市名或 adcode，为空则不加 city 参数（全国搜索仅作兜底）
     */
    public List<LocationTipDTO> inputTips(String keyword, String city) {
        if (!hasText(keyword) || keyword.trim().length() < 2) {
            return List.of();
        }

        String cacheKey = keyword.trim() + "|" + (hasText(city) ? city.trim() : "");

        // 检查缓存
        CacheEntry<List<LocationTipDTO>> cached = tipCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            if (cached.value == null) {
                // 上次请求失败，缓存期内不重试
                return List.of();
            }
            return cached.value;
        }

        try {
            // ★ 限流检查（缓存命中时不计入 QPS）
            acquireRateLimit();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/assistant/inputtips")
                    .queryParam("key", amapKey)
                    .queryParam("keywords", keyword.trim())
                    .queryParam("output", "JSON");

            if (hasText(city)) {
                builder.queryParam("city", city.trim());
            }

            java.net.URI uri = builder.build().encode().toUri();
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            String infocode = root.path("infocode").asText();

            if (!"1".equals(status)) {
                String info = root.path("info").asText("");
                System.out.println("[AMAP] inputtips FAIL keyword=" + keyword + " city=" + city
                        + " status=" + status + " infocode=" + infocode + " info=" + info);

                // ★ infocode=10021 表示 QPS 超限，抛出异常让前端显示友好提示
                if ("10021".equals(infocode) || info.contains("CUQPS") || info.contains("LIMIT")) {
                    tipCache.put(cacheKey, CacheEntry.fail(15_000));
                    throw new AmapRateLimitException("地点查询过于频繁，请稍后再试");
                }

                tipCache.put(cacheKey, CacheEntry.fail(30_000));
                return List.of();
            }

            List<LocationTipDTO> result = new ArrayList<>();
            JsonNode tips = root.path("tips");
            int total = tips.size();
            for (JsonNode tip : tips) {
                String location = text(tip, "location");
                if ("[]".equals(location)) location = "";
                LocationTipDTO dto = new LocationTipDTO();
                dto.setName(text(tip, "name"));
                dto.setDistrict(text(tip, "district"));
                dto.setAdcode(text(tip, "adcode"));
                dto.setLocation(location);
                dto.setAddress(text(tip, "address"));
                dto.setCityName(resolveCityName(dto.getDistrict(), dto.getName()));
                if (hasText(dto.getName()) || hasText(dto.getDistrict())) {
                    result.add(dto);
                }
            }

            String firstNames = result.stream()
                    .limit(3)
                    .map(LocationTipDTO::getName)
                    .collect(Collectors.joining(", "));
            if (total == 0) {
                // total=0 可能因为 API key QPS 超限、关键字过于宽泛或编码问题
                System.out.println("[AMAP] inputtips EMPTY keyword=" + keyword + " city=" + city
                        + " total=0 infocode=" + infocode
                        + " (可能 API key QPS 限制或 daily quota 耗尽)");
            } else {
                System.out.println("[AMAP] inputtips keyword=" + keyword + " city=" + city
                        + " total=" + total + " matched=" + result.size()
                        + " infocode=" + infocode
                        + " firstNames=[" + firstNames + "]");
            }

            tipCache.put(cacheKey, CacheEntry.ok(result, 30_000));
            return result;
        } catch (AmapRateLimitException e) {
            System.out.println("[AMAP] inputtips RATE_LIMITED keyword=" + keyword + " city=" + city);
            // 限流时缓存更短时间（10 秒），让前端更快重试
            tipCache.put(cacheKey, CacheEntry.fail(10_000));
            throw e; // 重新抛出，让上层返回友好提示给前端
        } catch (Exception e) {
            System.out.println("[AMAP] inputtips ERROR keyword=" + keyword + " city=" + city
                    + " error=" + e.getMessage());
            tipCache.put(cacheKey, CacheEntry.fail(30_000));
            return List.of();
        }
    }

    public List<LocationInfo> resolvePlace(String keyword, String city, String adcode) {
        if (!hasText(keyword)) {
            return List.of();
        }
        try {
            acquireRateLimit();
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/place/text")
                    .queryParam("key", amapKey)
                    .queryParam("keywords", keyword.trim())
                    .queryParam("city", firstText(adcode, city, ""))
                    .queryParam("citylimit", hasText(city) || hasText(adcode))
                    .queryParam("offset", 10)
                    .queryParam("page", 1)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                System.out.println("[AMAP] place resolve FAIL keyword=" + keyword
                        + " info=" + root.path("info").asText()
                        + " infocode=" + root.path("infocode").asText());
                return List.of();
            }
            List<LocationInfo> result = new ArrayList<>();
            for (JsonNode poi : root.path("pois")) {
                String location = text(poi, "location");
                if (!hasText(location)) {
                    continue;
                }
                LocationInfo info = new LocationInfo();
                info.setName(firstText(text(poi, "name"), keyword));
                info.setFormattedAddress((text(poi, "pname") + text(poi, "cityname") + text(poi, "adname") + text(poi, "address")).trim());
                info.setLocation(location);
                info.setAdcode(text(poi, "adcode"));
                info.setProvince(text(poi, "pname"));
                info.setCity(text(poi, "cityname"));
                info.setDistrict(text(poi, "adname"));
                result.add(info);
            }
            System.out.println("[AMAP] place resolve keyword=" + keyword + " count=" + result.size());
            return result;
        } catch (Exception e) {
            System.out.println("[AMAP] place resolve ERROR keyword=" + keyword + " error=" + e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析目的地经纬度。优先使用高德 text search，失败则用 inputtips。
     * 返回 null 表示无法解析。
     */
    public LocationInfo resolveDestination(String placeName, String city, String adcode) {
        if (!hasText(placeName)) {
            return null;
        }
        // 优先用 place text 精确解析
        List<LocationInfo> places = resolvePlace(placeName, city, adcode);
        if (!places.isEmpty()) {
            return places.get(0);
        }
        // 回退：用 inputtips 取第一个有效结果
        List<LocationTipDTO> tips = inputTips(placeName, city);
        if (!tips.isEmpty()) {
            LocationTipDTO tip = tips.get(0);
            LocationInfo info = new LocationInfo();
            info.setName(tip.getName());
            info.setFormattedAddress(tip.getAddress());
            info.setLocation(tip.getLocation());
            info.setAdcode(tip.getAdcode());
            info.setDistrict(tip.getDistrict());
            info.setCity(tip.getCityName());
            return info;
        }
        return null;
    }

    public LocationInfo reverseGeocode(String lngLat) {
        if (!hasLngLat(lngLat)) {
            throw new IllegalArgumentException("逆地理编码必须提供合法经纬度");
        }
        try {
            acquireRateLimit();
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                    .queryParam("key", amapKey)
                    .queryParam("location", lngLat)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                System.out.println("[AMAP] reverse geocode FAIL location=" + lngLat
                        + " info=" + root.path("info").asText());
                return null;
            }
            JsonNode regeocode = root.path("regeocode");
            JsonNode address = regeocode.path("addressComponent");
            LocationInfo info = new LocationInfo();
            info.setFormattedAddress(text(regeocode, "formatted_address"));
            info.setLocation(lngLat);
            info.setAdcode(text(address, "adcode"));
            info.setProvince(text(address, "province"));
            info.setCity(text(address, "city"));
            info.setDistrict(text(address, "district"));
            info.setName(info.getFormattedAddress());
            System.out.println("[AMAP] reverse geocode location=" + lngLat
                    + " city=" + info.getCity() + " district=" + info.getDistrict());
            return info;
        } catch (Exception e) {
            System.out.println("[AMAP] reverse geocode ERROR location=" + lngLat + " error=" + e.getMessage());
            return null;
        }
    }

    private String resolveCityName(String district, String name) {
        String text = firstText(district, name, "");
        int index = text.indexOf("市");
        return index >= 0 ? text.substring(0, index + 1) : text;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return "";
        if (value.isArray()) return value.isEmpty() ? "" : value.get(0).asText("");
        return value.asText("");
    }

    private String firstText(String... values) {
        for (String value : values) if (hasText(value)) return value.trim();
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasLngLat(String value) {
        if (!hasText(value) || !value.contains(",")) return false;
        String[] parts = value.split(",");
        if (parts.length != 2) return false;
        try {
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * ★ 滑动窗口限流（全局共享）：每分钟最多 MAX_CALLS_PER_MINUTE 次（≈3 QPS）。
     * 超过限制抛出 AmapRateLimitException。声明为 public static 供其他 Amap Service 调用。
     */
    public static synchronized void acquireRateLimit() {
        long now = System.currentTimeMillis();
        // 清理 1 分钟前的记录
        while (!recentCallTimestamps.isEmpty() && now - recentCallTimestamps.getFirst() > 60_000) {
            recentCallTimestamps.removeFirst();
        }
        if (recentCallTimestamps.size() >= MAX_CALLS_PER_MINUTE) {
            throw new AmapRateLimitException("地点查询过于频繁，请稍后再试");
        }
        recentCallTimestamps.addLast(now);
    }

    /**
     * ★ 高德限流异常，由调用方捕获后返回友好提示。
     */
    public static class AmapRateLimitException extends RuntimeException {
        public AmapRateLimitException(String message) {
            super(message);
        }
    }

    /**
     * 简单 TTL 缓存条目。value 为 null 时表示失败缓存。
     */
    private static class CacheEntry<T> {
        final T value;
        final long expireAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        static <T> CacheEntry<T> ok(T value, long ttlMs) {
            return new CacheEntry<>(value, ttlMs);
        }

        static <T> CacheEntry<T> fail(long ttlMs) {
            return new CacheEntry<>(null, ttlMs);
        }
    }
}
