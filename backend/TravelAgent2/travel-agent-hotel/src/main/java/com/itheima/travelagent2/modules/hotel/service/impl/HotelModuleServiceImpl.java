package com.itheima.travelagent2.modules.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.amap.AmapGeoService;
import com.itheima.travelagent2.modules.booking.dto.HotelDetailDTO;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.service.HotelService;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelOrderCreateRequest;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.dto.LocationTipDTO;
import com.itheima.travelagent2.modules.hotel.entity.HotelOrder;
import com.itheima.travelagent2.modules.hotel.mapper.HotelOrderMapper;
import com.itheima.travelagent2.modules.hotel.service.HotelApiClient;
import com.itheima.travelagent2.modules.hotel.service.HotelModuleService;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelModuleServiceImpl implements HotelModuleService {
    private final HotelApiClient hotelApiClient;
    private final HotelService localHotelService;
    private final HotelOrderMapper hotelOrderMapper;
    private final AmapGeoService amapGeoService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amap.key:${integration.amap-key:}}")
    private String amapKey;

    public HotelModuleServiceImpl(HotelApiClient hotelApiClient,
                                  HotelService localHotelService,
                                  HotelOrderMapper hotelOrderMapper,
                                  AmapGeoService amapGeoService) {
        this.hotelApiClient = hotelApiClient;
        this.localHotelService = localHotelService;
        this.hotelOrderMapper = hotelOrderMapper;
        this.amapGeoService = amapGeoService;
    }

    @Override
    public List<HotelItemDTO> searchHotels(HotelSearchRequest request) {
        List<HotelItemDTO> amapHotels = searchAmapHotels(request);
        if (!amapHotels.isEmpty()) {
            return applyPreference(amapHotels, request);
        }

        // 保留第三方酒店供应商适配层，但不强依赖 Amadeus Self-Service。
        // 后续切携程、Expedia、Amadeus Enterprise 时只替换 HotelApiClient 实现。
        List<HotelItemDTO> supplierHotels = hotelApiClient.search(request);
        if (!supplierHotels.isEmpty()) {
            return applyPreference(supplierHotels, request);
        }
        return applyPreference(localFallback(request), request);
    }

    @Override
    public HotelDetailDTO detail(Long id) {
        return localHotelService.detail(id);
    }

    /**
     * 高德 POI 酒店搜索。
     * 要求：优先使用目的地经纬度 + radius + types + citylimit=true。
     * 没有经纬度时先解析目的地，不允许直接全国搜索"酒店"。
     */
    private List<HotelItemDTO> searchAmapHotels(HotelSearchRequest request) {
        if (isBlank(amapKey)) {
            return List.of();
        }

        // 确定城市名
        String cityName = firstText(request.getCityName(), request.getAdcode(), "");
        if (isBlank(cityName)) {
            System.out.println("[AMAP] hotel search SKIP: no cityName");
            return List.of();
        }

        // 确定经纬度：优先使用请求中的 lat/lng
        BigDecimal lat = request.getLatitude();
        BigDecimal lng = request.getLongitude();

        // 如果没有经纬度，尝试解析目的地
        if (lat == null || lng == null) {
            LocationInfo resolved = amapGeoService.resolveDestination(cityName, cityName, request.getAdcode());
            if (resolved != null && hasText(resolved.getLocation())) {
                String[] parts = resolved.getLocation().split(",");
                if (parts.length == 2) {
                    try {
                        lng = new BigDecimal(parts[0]);
                        lat = new BigDecimal(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 仍然没有经纬度 → 不允许全国搜索"酒店"，返回空
        if (lat == null || lng == null) {
            System.out.println("[AMAP] hotel search SKIP: no lat/lng for city=" + cityName);
            return List.of();
        }

        try {
            AmapGeoService.acquireRateLimit();

            // ★ 关键修复：使用 location + radius + types + citylimit
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/place/around")
                    .queryParam("key", amapKey)
                    .queryParam("location", lng + "," + lat)
                    .queryParam("radius", 3000)
                    .queryParam("types", "100100|100101|100102|100103|100105")
                    .queryParam("keywords", "酒店")
                    .queryParam("citylimit", "true")
                    .queryParam("offset", 20)
                    .queryParam("page", 1)
                    .queryParam("extensions", "all")
                    .queryParam("output", "JSON");

            String response = restTemplate.getForObject(builder.build().encode().toUriString(), String.class);
            JsonNode root = objectMapper.readTree(response);

            if (!"1".equals(root.path("status").asText())) {
                System.out.println("[AMAP] hotel search FAIL info=" + root.path("info").asText()
                        + " infocode=" + root.path("infocode").asText());
                return List.of();
            }

            JsonNode pois = root.path("pois");
            int total = pois.size();
            List<HotelItemDTO> hotels = new ArrayList<>();
            int index = 0;
            for (JsonNode poi : pois) {
                HotelItemDTO item = fromAmapPoi(poi, cityName, index++);
                if (item != null) {
                    hotels.add(item);
                }
            }

            // ★ 精简日志
            String firstNames = hotels.stream()
                    .limit(3)
                    .map(HotelItemDTO::getName)
                    .collect(Collectors.joining(", "));
            System.out.println("[AMAP] hotel search keyword=酒店 city=" + cityName
                    + " location=" + lng + "," + lat
                    + " radius=3000 count=" + total
                    + " firstNames=[" + firstNames + "]");

            return hotels;
        } catch (Exception ex) {
            System.out.println("[AMAP] hotel search ERROR city=" + cityName + " error=" + ex.getMessage());
            return List.of();
        }
    }

    /**
     * 从高德 POI 构造 HotelItemDTO。
     * 只保留与目标城市同城或在搜索半径内的结果。
     */
    private HotelItemDTO fromAmapPoi(JsonNode poi, String targetCity, int index) {
        String name = poi.path("name").asText("");
        String location = poi.path("location").asText("");
        if (isBlank(name) || isBlank(location)) {
            return null;
        }

        // 校验城市匹配：跳过明显不属于目标城市的 POI
        String poiCity = poi.path("cityname").asText("");
        String poiPname = poi.path("pname").asText("");
        if (hasText(targetCity) && hasText(poiCity)) {
            String normalizedTarget = targetCity.replace("市", "").trim();
            String normalizedPoi = poiCity.replace("市", "").trim();
            if (!normalizedPoi.contains(normalizedTarget) && !normalizedTarget.contains(normalizedPoi)) {
                // 不在同城，跳过（例如搜索杭州时跳过北京、天津的酒店）
                return null;
            }
        }

        String[] point = location.split(",");
        BigDecimal longitude = point.length > 0 ? decimal(point[0]) : BigDecimal.ZERO;
        BigDecimal latitude = point.length > 1 ? decimal(point[1]) : BigDecimal.ZERO;
        int seed = Math.abs(name.hashCode() + index);
        BigDecimal price = new BigDecimal(260 + seed % 620);
        BigDecimal rating = new BigDecimal("4." + (seed % 9));

        HotelItemDTO item = new HotelItemDTO();
        item.setHotelId(poi.path("id").asText(name));
        item.setName(name);
        item.setHotelName(name);
        item.setAddress(firstText(poi.path("address").asText(""),
                poiPname + poiCity + poi.path("adname").asText("")));
        item.setLatitude(latitude);
        item.setLongitude(longitude);
        item.setCoverImage(amapCoverImage(index));
        item.setStarLevel(3 + seed % 3);
        item.setRating(rating);
        item.setScore(rating);
        item.setReviewCount(120 + seed % 1200);
        item.setLowestPrice(price);
        item.setMinPrice(price);
        item.setFacilities(List.of("高德真实地点", "位置已校验", facilityByPreference(null)));
        item.setTags(String.join(",", item.getFacilities()));
        item.setDescription("来自高德地图 POI 的真实酒店地点，价格、评分和图片为本地演示规则补充。");
        item.setDistanceText(distanceText(poi.path("distance").asText("")));
        item.setSource("AMAP_POI");
        return item;
    }

    private String amapCoverImage(int index) {
        List<String> images = List.of(
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=900&q=80",
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=900&q=80"
        );
        return images.get(Math.floorMod(index, images.size()));
    }

    private String facilityByPreference(String preference) {
        if (preference == null) {
            return "舒适优先";
        }
        if (preference.contains("低价")) {
            return "低价优先";
        }
        if (preference.contains("评分")) {
            return "评分优先";
        }
        if (preference.contains("距离")) {
            return "距离优先";
        }
        return "舒适优先";
    }

    private String distanceText(String distance) {
        if (isBlank(distance)) {
            return "目的地附近";
        }
        return "距目的地约 " + distance + " 米";
    }

    @Override
    public HotelOrder createOrder(HotelOrderCreateRequest request) {
        HotelOrder order = new HotelOrder();
        order.setUserId(isBlank(request.getUserId()) ? "test_user" : request.getUserId());
        order.setHotelId(request.getHotelId());
        order.setHotelName(request.getHotelName());
        order.setCity(request.getCity());
        order.setAddress(request.getAddress());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setAdults(defaultInt(request.getAdults(), 2));
        order.setChildren(defaultInt(request.getChildren(), 0));
        order.setRooms(defaultInt(request.getRooms(), 1));
        order.setRoomType(isBlank(request.getRoomType()) ? "标准房" : request.getRoomType());
        order.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        order.setStatus("PENDING");
        hotelOrderMapper.insert(order);
        return order;
    }

    @Override
    public List<LocationTipDTO> locationTips(String keyword) {
        return locationTips(keyword, "");
    }

    @Override
    public List<LocationTipDTO> locationTips(String keyword, String city) {
        if (isBlank(keyword)) {
            return List.of();
        }
        try {
            return amapGeoService.inputTips(keyword, city);
        } catch (AmapGeoService.AmapRateLimitException ex) {
            // 让限流异常传播到 Controller 层，返回友好提示给前端
            throw ex;
        } catch (Exception ex) {
            System.out.println("[AMAP] 地点提示失败 keyword=" + keyword + " city=" + city + " error=" + ex.getMessage());
            return List.of();
        }
    }

    private List<HotelItemDTO> localFallback(HotelSearchRequest request) {
        String city = firstText(request.getCityName(), request.getAdcode(), "");
        // ★ 如果 cityName 和目的地经纬度都没有，不允许全表查询
        if (!hasText(city) && request.getLatitude() == null && request.getLongitude() == null) {
            System.out.println("[HOTEL] localFallback SKIP: no cityName and no lat/lng");
            return List.of();
        }
        String normalizedCity = com.itheima.travelagent2.modules.hub.service.impl.TransportHubServiceImpl
                .normalizeCityName(city);
        List<Hotel> hotels = localHotelService.search(normalizedCity, normalizedCity, request.getMinPrice(), request.getMaxPrice());
        // ★ 如果精确匹配无结果，不再全表查询；只有明确有城市名时才宽泛搜索
        if (hotels.isEmpty() && hasText(normalizedCity)) {
            // 用宽泛 LIKE 兜底，但仍限定城市
            hotels = localHotelService.search(normalizedCity.substring(0, Math.min(2, normalizedCity.length())),
                    normalizedCity, request.getMinPrice(), request.getMaxPrice());
        }
        if (hotels.isEmpty()) {
            System.out.println("[HOTEL] localFallback EMPTY city=" + normalizedCity + " raw=" + city);
            return List.of();
        }
        // 同城过滤 + 限制最多 8 条
        String filterCity = normalizedCity;
        List<Hotel> filtered = hotels.stream()
                .filter(h -> h.getCity() != null
                        && com.itheima.travelagent2.modules.hub.service.impl.TransportHubServiceImpl
                        .normalizeCityName(h.getCity()).contains(filterCity))
                .limit(8)
                .toList();
        System.out.println("[HOTEL] localFallback city=" + normalizedCity
                + " total=" + hotels.size() + " filtered=" + filtered.size());
        return filtered.stream().map(this::toItem).toList();
    }

    private HotelItemDTO toItem(Hotel hotel) {
        HotelItemDTO item = new HotelItemDTO();
        item.setId(hotel.getId());
        item.setHotelId(String.valueOf(hotel.getId()));
        item.setName(hotel.getName());
        item.setHotelName(hotel.getName());
        item.setAddress(hotel.getAddress());
        item.setCoverImage(hotel.getCoverImage());
        item.setStarLevel(hotel.getStarLevel());
        item.setRating(hotel.getScore());
        item.setScore(hotel.getScore());
        item.setReviewCount(80 + Math.abs(hotel.getName().hashCode() % 420));
        item.setLowestPrice(hotel.getMinPrice());
        item.setMinPrice(hotel.getMinPrice());
        item.setTags(hotel.getTags());
        item.setFacilities(splitTags(hotel.getTags()));
        item.setDescription(hotel.getDescription());
        item.setDistanceText("距目的地约 1-5 公里");
        item.setSource("LOCAL_FALLBACK");
        return item;
    }

    private List<HotelItemDTO> applyPreference(List<HotelItemDTO> hotels, HotelSearchRequest request) {
        if (request == null || isBlank(request.getPreference())) {
            return hotels;
        }
        String preference = request.getPreference();
        if (preference.contains("低价")) {
            return hotels.stream().sorted((a, b) -> safePrice(a).compareTo(safePrice(b))).toList();
        }
        if (preference.contains("评分")) {
            return hotels.stream().sorted((a, b) -> safeRating(b).compareTo(safeRating(a))).toList();
        }
        return hotels;
    }

    private List<String> splitTags(String tags) {
        if (isBlank(tags)) {
            return List.of("舒适优先", "可预订");
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private BigDecimal safePrice(HotelItemDTO item) {
        return item.getLowestPrice() == null ? BigDecimal.ZERO : item.getLowestPrice();
    }

    private BigDecimal safeRating(HotelItemDTO item) {
        return item.getRating() == null ? BigDecimal.ZERO : item.getRating();
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null || value < 0 ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
