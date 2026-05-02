package com.itheima.travelagent2.modules.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.dto.RouteQueryDTO;
import com.itheima.travelagent2.modules.route.dto.WeatherInfo;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.route.mapper.RouteMapper;
import com.itheima.travelagent2.modules.route.service.RouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements RouteService {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String REGEOCODE_URL = "https://restapi.amap.com/v3/geocode/regeo";
    private static final String PLACE_TEXT_URL = "https://restapi.amap.com/v3/place/text";
    private static final String DRIVING_URL = "https://restapi.amap.com/v3/direction/driving";
    private static final String DISTRICT_URL = "https://restapi.amap.com/v3/config/district";
    private static final String WEATHER_URL = "https://restapi.amap.com/v3/weather/weatherInfo";

    private final ObjectMapper objectMapper;
    private final ZhipuAiClient zhipuAiClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${amap.key:${integration.amap-key:}}")
    private String amapKey;

    @Value("${integration.weather-key:${amap.key:${integration.amap-key:}}}")
    private String weatherKey;

    public RouteServiceImpl(ObjectMapper objectMapper, ZhipuAiClient zhipuAiClient) {
        this.objectMapper = objectMapper;
        this.zhipuAiClient = zhipuAiClient;
    }

    @Override
    public RoutePlanResponseDTO queryRoutes(RouteQueryDTO dto) {
        validate(dto);

        LocationInfo originInfo = resolveLocation(dto, true);
        LocationInfo destinationInfo = resolveLocation(dto, false);
        WeatherInfo startWeather = queryWeather(originInfo.getAdcode());
        WeatherInfo endWeather = queryWeather(destinationInfo.getAdcode());

        List<Route> routes = new ArrayList<>();
        routes.add(queryDriving(dto, originInfo.getLocation(), destinationInfo.getLocation(), "SMART",
                "\u667a\u80fd\u63a8\u8350\u65b9\u6848", "10"));
        routes.add(queryDriving(dto, originInfo.getLocation(), destinationInfo.getLocation(), "FAST",
                "\u5feb\u901f\u51fa\u884c\u65b9\u6848", "0"));
        routes.add(queryDriving(dto, originInfo.getLocation(), destinationInfo.getLocation(), "ECO",
                "\u7ecf\u6d4e\u51fa\u884c\u65b9\u6848", "1"));

        enrichRoutesWithAi(dto, originInfo, destinationInfo, startWeather, endWeather, routes);

        for (Route route : routes) {
            this.save(route);
        }

        RoutePlanResponseDTO response = new RoutePlanResponseDTO();
        response.setStartLocation(originInfo);
        response.setEndLocation(destinationInfo);
        response.setStartWeather(startWeather);
        response.setEndWeather(endWeather);
        response.setRoutes(routes);
        response.setServiceCapabilities(List.of(
                "\u5b9e\u65f6\u4f4d\u7f6e\u5b88\u62a4",
                "\u8f68\u8ff9\u56de\u653e",
                "\u7535\u5b50\u56f4\u680f\u63d0\u9192",
                "\u5f02\u5e38\u505c\u7559\u4e0e\u504f\u822a\u63d0\u9192"
        ));
        return response;
    }

    private LocationInfo resolveLocation(RouteQueryDTO dto, boolean start) {
        String location = start ? dto.getStartLocation() : dto.getEndLocation();
        String point = start ? dto.getStartPoint() : dto.getEndPoint();
        if (notBlank(location) && location.contains(",")) {
            LocationInfo info = new LocationInfo();
            info.setName(point);
            info.setFormattedAddress(point);
            info.setLocation(location.trim());
            info.setAdcode(start ? dto.getStartAdcode() : dto.getEndAdcode());
            info.setCity(start ? dto.getStartCity() : dto.getEndCity());
            if (!notBlank(info.getAdcode())) {
                LocationInfo reverseInfo = reverseGeocode(location.trim(), point);
                if (reverseInfo != null) {
                    reverseInfo.setName(point);
                    reverseInfo.setLocation(location.trim());
                    return reverseInfo;
                }
            }
            return info;
        }
        return geocode(point);
    }

    @Override
    public Route getRouteDetail(Long routeId) {
        return this.getById(routeId);
    }

    @Override
    public List<Route> listAll() {
        QueryWrapper<Route> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return this.list(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        this.removeById(id);
    }

    private LocationInfo geocode(String address) {
        String cleanAddress = address == null ? "" : address.trim();
        String response = "";
        try {
            String url = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                    .queryParam("key", amapKey)
                    .queryParam("address", cleanAddress)
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            response = restTemplate.getForObject(url, String.class);
            System.out.println("AMap geocode response for [" + cleanAddress + "]: " + response);

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            String info = root.path("info").asText();
            String infocode = root.path("infocode").asText();

            if (!"1".equals(status)) {
                if ("ENGINE_RESPONSE_DATA_ERROR".equals(info)) {
                    LocationInfo poi = searchPoi(cleanAddress, response);
                    if (poi != null) {
                        return poi;
                    }
                    LocationInfo builtInPlace = builtInPlaceFallback(cleanAddress);
                    if (builtInPlace != null) {
                        System.out.println("AMap geocode ENGINE_RESPONSE_DATA_ERROR, use built-in POI fallback for ["
                                + cleanAddress + "]: " + builtInPlace.getLocation()
                                + ", adcode=" + builtInPlace.getAdcode());
                        return builtInPlace;
                    }
                    LocationInfo builtIn = builtInCityFallback(cleanAddress);
                    if (builtIn != null) {
                        System.out.println("AMap geocode ENGINE_RESPONSE_DATA_ERROR, use built-in city fallback for ["
                                + cleanAddress + "]: " + builtIn.getLocation() + ", adcode=" + builtIn.getAdcode());
                        return builtIn;
                    }
                    return districtCenter(cleanAddress, response);
                }
                throw new IllegalStateException("AMap geocode failed, info=" + info
                        + ", infocode=" + infocode + ", response=" + response);
            }

            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                LocationInfo poi = searchPoi(cleanAddress, response);
                if (poi != null) {
                    return poi;
                }
                LocationInfo builtInPlace = builtInPlaceFallback(cleanAddress);
                if (builtInPlace != null) {
                    System.out.println("AMap geocode empty, use built-in POI fallback for ["
                            + cleanAddress + "]: " + builtInPlace.getLocation()
                            + ", adcode=" + builtInPlace.getAdcode());
                    return builtInPlace;
                }
                LocationInfo builtIn = builtInCityFallback(cleanAddress);
                if (builtIn != null) {
                    System.out.println("AMap geocode empty, use built-in city fallback for ["
                            + cleanAddress + "]: " + builtIn.getLocation() + ", adcode=" + builtIn.getAdcode());
                    return builtIn;
                }
                return districtCenter(cleanAddress, response);
            }

            JsonNode first = geocodes.get(0);
            String location = first.path("location").asText();
            if (location == null || location.isBlank()) {
                LocationInfo poi = searchPoi(cleanAddress, response);
                if (poi != null) {
                    return poi;
                }
                LocationInfo builtInPlace = builtInPlaceFallback(cleanAddress);
                if (builtInPlace != null) {
                    System.out.println("AMap geocode blank location, use built-in POI fallback for ["
                            + cleanAddress + "]: " + builtInPlace.getLocation()
                            + ", adcode=" + builtInPlace.getAdcode());
                    return builtInPlace;
                }
                LocationInfo builtIn = builtInCityFallback(cleanAddress);
                if (builtIn != null) {
                    System.out.println("AMap geocode blank location, use built-in city fallback for ["
                            + cleanAddress + "]: " + builtIn.getLocation() + ", adcode=" + builtIn.getAdcode());
                    return builtIn;
                }
                return districtCenter(cleanAddress, response);
            }

            LocationInfo infoDto = new LocationInfo();
            infoDto.setName(cleanAddress);
            infoDto.setFormattedAddress(text(first, "formatted_address"));
            infoDto.setLocation(location);
            infoDto.setAdcode(text(first, "adcode"));
            infoDto.setProvince(text(first, "province"));
            infoDto.setCity(text(first, "city"));
            infoDto.setDistrict(text(first, "district"));
            return infoDto;
        } catch (Exception e) {
            if (e instanceof IllegalStateException
                    && e.getMessage() != null
                    && e.getMessage().startsWith("AMap geocode failed")) {
                throw new RuntimeException("Place geocode failed: " + cleanAddress + ", reason=" + e.getMessage(), e);
            }
            try {
                LocationInfo poi = searchPoi(cleanAddress, response);
                if (poi != null) {
                    return poi;
                }
                LocationInfo builtInPlace = builtInPlaceFallback(cleanAddress);
                if (builtInPlace != null) {
                    System.out.println("AMap geocode exception, use built-in POI fallback for ["
                            + cleanAddress + "]: " + builtInPlace.getLocation()
                            + ", adcode=" + builtInPlace.getAdcode());
                    return builtInPlace;
                }
                return districtCenter(cleanAddress, response);
            } catch (Exception fallbackError) {
                throw new RuntimeException("Place geocode failed: " + cleanAddress
                        + ", reason=" + e.getMessage()
                        + ", fallbackReason=" + fallbackError.getMessage(), fallbackError);
            }
        }
    }

    private LocationInfo searchPoi(String address, String geocodeResponse) {
        if (address == null || address.trim().isBlank()) {
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(PLACE_TEXT_URL)
                    .queryParam("key", amapKey)
                    .queryParam("keywords", address.trim())
                    .queryParam("city", inferCity(address))
                    .queryParam("citylimit", false)
                    .queryParam("offset", 1)
                    .queryParam("page", 1)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            System.out.println("AMap POI response for [" + address + "]: " + response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode pois = root.path("pois");
            if (!"1".equals(root.path("status").asText()) || !pois.isArray() || pois.isEmpty()) {
                System.out.println("AMap POI empty for [" + address + "], geocodeResponse=" + geocodeResponse);
                return null;
            }

            JsonNode first = pois.get(0);
            String location = text(first, "location");
            if (location.isBlank()) {
                return null;
            }

            LocationInfo info = new LocationInfo();
            info.setName(text(first, "name").isBlank() ? address : text(first, "name"));
            info.setFormattedAddress(buildPoiAddress(first, address));
            info.setLocation(location);
            info.setAdcode(text(first, "adcode"));
            info.setProvince(text(first, "pname"));
            info.setCity(text(first, "cityname"));
            info.setDistrict(text(first, "adname"));
            return info;
        } catch (Exception e) {
            System.out.println("AMap POI query failed for [" + address + "]: " + e.getMessage());
            return null;
        }
    }

    private LocationInfo reverseGeocode(String location, String originalAddress) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(REGEOCODE_URL)
                    .queryParam("key", amapKey)
                    .queryParam("location", location)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            System.out.println("AMap regeo response for [" + location + "]: " + response);

            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                return null;
            }

            JsonNode regeocode = root.path("regeocode");
            JsonNode addressComponent = regeocode.path("addressComponent");
            LocationInfo info = new LocationInfo();
            info.setName(originalAddress);
            info.setFormattedAddress(text(regeocode, "formatted_address"));
            info.setLocation(location);
            info.setAdcode(text(addressComponent, "adcode"));
            info.setProvince(text(addressComponent, "province"));
            info.setCity(text(addressComponent, "city"));
            info.setDistrict(text(addressComponent, "district"));
            return info;
        } catch (Exception e) {
            System.out.println("AMap regeo query failed for [" + location + "]: " + e.getMessage());
            return null;
        }
    }

    private String buildPoiAddress(JsonNode poi, String fallback) {
        String province = text(poi, "pname");
        String city = text(poi, "cityname");
        String district = text(poi, "adname");
        String address = text(poi, "address");
        String name = text(poi, "name");
        String value = (province + city + district + address + name).trim();
        return value.isBlank() ? fallback : value;
    }

    private String inferCity(String address) {
        String normalized = normalizeCityName(address);
        if (normalized.contains("\u5357\u4eac")) {
            return "\u5357\u4eac";
        }
        if (normalized.contains("\u676d\u5dde")) {
            return "\u676d\u5dde";
        }
        if (normalized.contains("\u82cf\u5dde")) {
            return "\u82cf\u5dde";
        }
        if (normalized.contains("\u4e0a\u6d77")) {
            return "\u4e0a\u6d77";
        }
        if (normalized.contains("\u5317\u4eac")) {
            return "\u5317\u4eac";
        }
        return "";
    }

    private LocationInfo districtCenter(String address, String geocodeResponse) {
        String lastDistrictResponse = "";
        Exception lastError = null;

        for (String keyword : districtKeywords(address)) {
            try {
            String url = UriComponentsBuilder.fromHttpUrl(DISTRICT_URL)
                    .queryParam("key", amapKey)
                        .queryParam("keywords", keyword)
                    .queryParam("subdistrict", 0)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
                lastDistrictResponse = response;
                System.out.println("AMap district response for [" + keyword + "]: " + response);

            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException("AMap district failed, info=" + root.path("info").asText()
                        + ", infocode=" + root.path("infocode").asText()
                        + ", geocodeResponse=" + geocodeResponse
                        + ", districtResponse=" + response);
            }

            JsonNode districts = root.path("districts");
                if (districts.isArray() && !districts.isEmpty()) {
                    return toLocationInfoFromDistrict(address, districts.get(0));
                }
            } catch (Exception e) {
                lastError = e;
            }
        }

        LocationInfo builtIn = builtInCityFallback(address);
        if (builtIn != null) {
            System.out.println("AMap district empty, use built-in city fallback for [" + address + "]: "
                    + builtIn.getLocation() + ", adcode=" + builtIn.getAdcode());
            return builtIn;
        }

        String reason = lastError == null ? "AMap district returned empty districts" : lastError.getMessage();
        throw new RuntimeException("District fallback failed: " + address
                + ", reason=" + reason
                + ", geocodeResponse=" + geocodeResponse
                + ", districtResponse=" + lastDistrictResponse, lastError);
    }

    private Set<String> districtKeywords(String address) {
        String value = address == null ? "" : address.trim();
        String normalized = normalizeCityName(value);
        Set<String> keywords = new LinkedHashSet<>();
        if (!value.isBlank()) {
            keywords.add(value);
        }
        if (!normalized.isBlank()) {
            keywords.add(normalized);
            keywords.add(normalized + "\u5e02");
            keywords.add(normalized + "\u5730\u533a");
        }
        return keywords;
    }

    private LocationInfo toLocationInfoFromDistrict(String originalAddress, JsonNode district) {
        String name = text(district, "name");
        LocationInfo info = new LocationInfo();
        info.setName(name.isBlank() ? originalAddress : name);
        info.setFormattedAddress(name.isBlank() ? originalAddress : name);
        info.setLocation(text(district, "center"));
        info.setAdcode(text(district, "adcode"));
        info.setProvince(text(district, "province"));
        info.setCity(text(district, "city"));
        info.setDistrict(text(district, "district"));
        return info;
    }

    private LocationInfo builtInCityFallback(String address) {
        String name = normalizeCityName(address);
        if ("\u5357\u4eac".equals(name) || name.contains("\u5357\u4eac")) {
            return cityInfo(address, "\u6c5f\u82cf\u7701", "\u5357\u4eac\u5e02", "320100", "118.796877,32.060255");
        }
        if ("\u676d\u5dde".equals(name) || name.contains("\u676d\u5dde")) {
            return cityInfo(address, "\u6d59\u6c5f\u7701", "\u676d\u5dde\u5e02", "330100", "120.155070,30.274084");
        }
        if ("\u82cf\u5dde".equals(name) || name.contains("\u82cf\u5dde")) {
            return cityInfo(address, "\u6c5f\u82cf\u7701", "\u82cf\u5dde\u5e02", "320500", "120.585316,31.298886");
        }
        if ("\u4e0a\u6d77".equals(name) || name.contains("\u4e0a\u6d77")) {
            return cityInfo(address, "\u4e0a\u6d77\u5e02", "\u4e0a\u6d77\u5e02", "310000", "121.473667,31.230525");
        }
        if ("\u5317\u4eac".equals(name) || name.contains("\u5317\u4eac")) {
            return cityInfo(address, "\u5317\u4eac\u5e02", "\u5317\u4eac\u5e02", "110000", "116.407387,39.904179");
        }
        return null;
    }

    private LocationInfo builtInPlaceFallback(String address) {
        if (address == null) {
            return null;
        }
        String value = address.trim();
        if (value.contains("\u5357\u4eac\u4ea4\u901a\u804c\u4e1a\u6280\u672f\u5b66\u9662")) {
            return placeInfo(value,
                    "\u6c5f\u82cf\u7701",
                    "\u5357\u4eac\u5e02",
                    "\u6c5f\u5b81\u533a",
                    "320115",
                    "118.913889,31.910833",
                    "\u6c5f\u82cf\u7701\u5357\u4eac\u5e02\u6c5f\u5b81\u533a\u9f99\u7720\u5927\u9053\u9644\u8fd1");
        }
        return null;
    }

    private LocationInfo placeInfo(String originalAddress, String province, String city, String district,
                                   String adcode, String location, String formattedAddress) {
        LocationInfo info = new LocationInfo();
        info.setName(originalAddress);
        info.setFormattedAddress(formattedAddress);
        info.setLocation(location);
        info.setAdcode(adcode);
        info.setProvince(province);
        info.setCity(city);
        info.setDistrict(district);
        return info;
    }

    private LocationInfo cityInfo(String originalAddress, String province, String city, String adcode, String location) {
        LocationInfo info = new LocationInfo();
        info.setName(originalAddress);
        info.setFormattedAddress(city);
        info.setLocation(location);
        info.setAdcode(adcode);
        info.setProvince(province);
        info.setCity(city);
        info.setDistrict("");
        return info;
    }

    private String normalizeCityName(String address) {
        if (address == null) {
            return "";
        }
        String value = address.trim();
        return value
                .replace("\u5e02", "")
                .replace("\u7701", "")
                .replace("\u5730\u533a", "")
                .replace("\u7279\u522b\u884c\u653f\u533a", "")
                .replace("\u58ee\u65cf\u81ea\u6cbb\u533a", "")
                .replace("\u56de\u65cf\u81ea\u6cbb\u533a", "")
                .replace("\u7ef4\u543e\u5c14\u81ea\u6cbb\u533a", "")
                .replace("\u81ea\u6cbb\u533a", "");
    }

    private WeatherInfo queryWeather(String adcode) {
        if (adcode == null || adcode.isBlank()) {
            return null;
        }
        WeatherInfo weather = queryWeatherWithKey(adcode, weatherKey);
        if (weather != null) {
            return weather;
        }
        if (notBlank(amapKey) && !amapKey.equals(weatherKey)) {
            return queryWeatherWithKey(adcode, amapKey);
        }
        return null;
    }

    private WeatherInfo queryWeatherWithKey(String adcode, String key) {
        if (!notBlank(key)) {
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(WEATHER_URL)
                    .queryParam("key", key)
                    .queryParam("city", adcode)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            System.out.println("AMap weather response for [" + adcode + "] with key suffix ["
                    + key.substring(Math.max(0, key.length() - 6)) + "]: " + response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode lives = root.path("lives");
            if (!"1".equals(root.path("status").asText()) || !lives.isArray() || lives.isEmpty()) {
                return null;
            }

            JsonNode live = lives.get(0);
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
            return weather;
        } catch (Exception e) {
            System.out.println("AMap weather query failed for [" + adcode + "]: " + e.getMessage());
            return null;
        }
    }

    private void enrichRoutesWithAi(RouteQueryDTO dto,
                                    LocationInfo originInfo,
                                    LocationInfo destinationInfo,
                                    WeatherInfo startWeather,
                                    WeatherInfo endWeather,
                                    List<Route> routes) {
        String systemPrompt = "\u4f60\u662f\u5546\u7528\u667a\u6167\u51fa\u884c\u7cfb\u7edf\u7684\u8def\u7ebf\u89c4\u5212\u52a9\u624b\u3002"
                + "\u5fc5\u987b\u57fa\u4e8e\u7528\u6237\u7684\u8d77\u7ec8\u70b9\u3001\u504f\u597d\u3001\u9ad8\u5fb7\u8fd4\u56de\u7684\u771f\u5b9e\u8ddd\u79bb\u3001\u8017\u65f6\u3001\u8d39\u7528\u548c\u5929\u6c14\u4fe1\u606f\u751f\u6210\u4e2d\u6587\u63a8\u8350\u3002"
                + "\u4e0d\u8981\u7f16\u9020\u65b0\u7684\u8ddd\u79bb\u3001\u65f6\u95f4\u6216\u4ef7\u683c\u3002";
        StringBuilder prompt = new StringBuilder();
        prompt.append("\u8d77\u70b9\uff1a").append(originInfo.getName()).append(" ").append(originInfo.getFormattedAddress()).append("\n");
        prompt.append("\u7ec8\u70b9\uff1a").append(destinationInfo.getName()).append(" ").append(destinationInfo.getFormattedAddress()).append("\n");
        prompt.append("\u7528\u6237\u504f\u597d\uff1a").append(dto.getPreference()).append("\n");
        prompt.append("\u8d77\u70b9\u5929\u6c14\uff1a").append(weatherText(startWeather)).append("\n");
        prompt.append("\u7ec8\u70b9\u5929\u6c14\uff1a").append(weatherText(endWeather)).append("\n");
        prompt.append("\u5907\u9009\u8def\u7ebf\uff1a\n");
        for (Route route : routes) {
            prompt.append(route.getRouteType()).append(" | ")
                    .append(route.getRouteName()).append(" | ")
                    .append(route.getDistance()).append("km | ")
                    .append(route.getDuration()).append("\u5206\u949f | ")
                    .append(route.getCost()).append("\u5143\n");
        }
        prompt.append("\n\u8bf7\u4e25\u683c\u6309\u4ee5\u4e0b\u683c\u5f0f\u8f93\u51fa\uff0c\u6bcf\u884c\u4e00\u6761\uff0c\u4e0d\u8981\u8f93\u51fa\u5176\u4ed6\u5185\u5bb9\uff1a\n");
        prompt.append("SMART\uff1a\u4e00\u53e5\u4e2d\u6587\u63a8\u8350\u7406\u7531\n");
        prompt.append("FAST\uff1a\u4e00\u53e5\u4e2d\u6587\u63a8\u8350\u7406\u7531\n");
        prompt.append("ECO\uff1a\u4e00\u53e5\u4e2d\u6587\u63a8\u8350\u7406\u7531");

        String aiText;
        try {
            aiText = zhipuAiClient.chat(systemPrompt, prompt.toString());
        } catch (Exception e) {
            throw new RuntimeException("\u8def\u7ebf\u63a8\u8350\u5fc5\u987b\u7531AI\u751f\u6210\uff0c\u4f46\u667a\u8c31\u6a21\u578b\u8c03\u7528\u5931\u8d25\uff1a" + e.getMessage(), e);
        }

        for (Route route : routes) {
            String reason = extractAiReason(aiText, route.getRouteType());
            if (!notBlank(reason)) {
                reason = extractFallbackAiReason(aiText, routes.indexOf(route));
            }
            if (!notBlank(reason)) {
                reason = aiText.trim();
            }
            route.setRecommendReason(reason);
            route.setDescription("\u57fa\u4e8e\u9ad8\u5fb7\u8def\u7ebf\u6570\u636e\uff1a\u5168\u7a0b\u7ea6 " + route.getDistance()
                    + " \u516c\u91cc\uff0c\u9884\u8ba1 " + route.getDuration()
                    + " \u5206\u949f\uff0c\u8fc7\u8def\u8d39\u7ea6 " + route.getCost() + " \u5143\u3002");
        }
    }

    private String weatherText(WeatherInfo weather) {
        if (weather == null) {
            return "\u6682\u65e0\u5929\u6c14\u6570\u636e";
        }
        return weather.getCity() + " " + weather.getWeather() + " "
                + weather.getTemperature() + "\u2103 \u6e7f\u5ea6"
                + weather.getHumidity() + "% \u98ce\u5411" + weather.getWinddirection()
                + " \u98ce\u529b" + weather.getWindpower();
    }

    private String extractAiReason(String aiText, String routeType) {
        if (!notBlank(aiText) || !notBlank(routeType)) {
            return "";
        }
        String[] lines = aiText.replace("\r", "").split("\n");
        for (String line : lines) {
            String value = line.trim().replaceFirst("^[0-9]+[.、)]\\s*", "");
            if (value.startsWith(routeType + ":") || value.startsWith(routeType + "\uff1a")) {
                return value.substring(value.indexOf(value.contains("\uff1a") ? '\uff1a' : ':') + 1).trim();
            }
        }
        return "";
    }

    private String extractFallbackAiReason(String aiText, int index) {
        if (!notBlank(aiText)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (String line : aiText.replace("\r", "").split("\n")) {
            String value = line.trim()
                    .replaceFirst("^[0-9]+[.、)]\\s*", "")
                    .replaceFirst("^[\\-*]\\s*", "");
            if (notBlank(value)) {
                lines.add(value);
            }
        }
        if (index >= 0 && index < lines.size()) {
            String value = lines.get(index);
            int colonIndex = Math.max(value.indexOf(':'), value.indexOf('\uff1a'));
            return colonIndex >= 0 ? value.substring(colonIndex + 1).trim() : value;
        }
        return "";
    }

    private Route queryDriving(RouteQueryDTO dto, String origin, String destination, String type,
                               String name, String strategy) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(DRIVING_URL)
                    .queryParam("key", amapKey)
                    .queryParam("origin", origin)
                    .queryParam("destination", destination)
                    .queryParam("strategy", strategy)
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build(false)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (!"1".equals(root.path("status").asText())) {
                throw new IllegalStateException(root.path("info").asText("AMap route query failed")
                        + ", infocode=" + root.path("infocode").asText()
                        + ", response=" + response);
            }

            JsonNode paths = root.path("route").path("paths");
            if (!paths.isArray() || paths.isEmpty()) {
                throw new IllegalStateException("No route returned by AMap, response=" + response);
            }

            JsonNode path = paths.get(0);
            double distanceKm = path.path("distance").asDouble(0D) / 1000D;
            int durationMin = Math.max(1, (int) Math.ceil(path.path("duration").asDouble(0D) / 60D));
            double cost = path.path("tolls").asDouble(0D);
            String trafficLights = path.path("traffic_lights").asText("");

            Route route = new Route();
            route.setUserId(dto.getUserId());
            route.setTripPlanId(dto.getTripPlanId());
            route.setStartPoint(dto.getStartPoint().trim());
            route.setEndPoint(dto.getEndPoint().trim());
            route.setRouteType(type);
            route.setRouteName(name);
            route.setDistance(round(distanceKm));
            route.setDuration(durationMin);
            route.setCost(round(cost));
            route.setDescription("\u9ad8\u5fb7\u8def\u7ebf\u8fd4\u56de\uff1a\u7ea6 " + route.getDistance() + " \u516c\u91cc\uff0c"
                    + durationMin + " \u5206\u949f\uff0c\u8fc7\u8def\u8d39\u9884\u4f30 " + route.getCost() + " \u5143"
                    + (trafficLights.isBlank() ? "\u3002" : "\uff0c\u7ea2\u7eff\u706f\u7ea6 " + trafficLights + " \u4e2a\u3002"));
            route.setRecommendReason("");
            route.setPreference(dto.getPreference());
            return route;
        } catch (Exception e) {
            throw new RuntimeException("AMap route planning failed. Check start point, end point or AMap key: "
                    + e.getMessage(), e);
        }
    }

    private void validate(RouteQueryDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Route query payload is required");
        }
        if (!notBlank(dto.getStartPoint())) {
            throw new IllegalArgumentException("Start point is required");
        }
        if (!notBlank(dto.getEndPoint())) {
            throw new IllegalArgumentException("End point is required");
        }
        if (!notBlank(amapKey)) {
            throw new IllegalStateException("AMap key is not configured");
        }
    }

    private String buildPreferenceText(String preference) {
        if (!notBlank(preference)) {
            return "";
        }
        return " Preference: " + preference.trim() + ".";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        if (value.isArray()) {
            return value.isEmpty() ? "" : value.get(0).asText("");
        }
        return value.asText("");
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }
}
