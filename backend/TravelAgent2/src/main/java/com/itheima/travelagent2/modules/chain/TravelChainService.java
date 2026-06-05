package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.amap.AmapWeatherService;
import com.itheima.travelagent2.modules.chain.dto.TravelChainRequestDTO;
import com.itheima.travelagent2.modules.hub.service.impl.TransportHubServiceImpl;
import com.itheima.travelagent2.modules.chain.dto.TravelChainResponseDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelGeoInfoDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelGeoPointDTO;
import com.itheima.travelagent2.modules.chain.dto.TripSegmentDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.service.HotelModuleService;
import com.itheima.travelagent2.modules.intent.IntentParseService;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.intent.dto.TravelIntentDTO;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.reminder.service.ReminderTemplateService;
import com.itheima.travelagent2.modules.risk.RiskService;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.dto.RouteQueryDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.route.service.RouteService;
import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.servicereport.service.TravelServiceReportService;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import com.itheima.travelagent2.modules.timeline.TravelTimelineService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class TravelChainService {
    private final IntentParseService intentParseService;
    private final RouteService routeService;
    private final HotelModuleService hotelModuleService;
    private final RiskService riskService;
    private final TravelTimelineService timelineService;
    private final TravelServiceReportService reportService;
    private final TripSegmentBuilderService tripSegmentBuilderService;
    private final ClarifyService clarifyService;
    private final SceneRuleService sceneRuleService;
    private final ReminderTemplateService reminderTemplateService;
    private final AmapWeatherService amapWeatherService;

    public TravelChainService(IntentParseService intentParseService,
                              RouteService routeService,
                              HotelModuleService hotelModuleService,
                              RiskService riskService,
                              TravelTimelineService timelineService,
                              TravelServiceReportService reportService,
                              TripSegmentBuilderService tripSegmentBuilderService,
                              ClarifyService clarifyService,
                              SceneRuleService sceneRuleService,
                              ReminderTemplateService reminderTemplateService,
                              AmapWeatherService amapWeatherService) {
        this.intentParseService = intentParseService;
        this.routeService = routeService;
        this.hotelModuleService = hotelModuleService;
        this.riskService = riskService;
        this.timelineService = timelineService;
        this.reportService = reportService;
        this.tripSegmentBuilderService = tripSegmentBuilderService;
        this.clarifyService = clarifyService;
        this.sceneRuleService = sceneRuleService;
        this.reminderTemplateService = reminderTemplateService;
        this.amapWeatherService = amapWeatherService;
    }

    public TravelChainResponseDTO plan(TravelChainRequestDTO request) {
        String userId = request.getUserId() == null || request.getUserId().isBlank() ? "test_user" : request.getUserId();
        TravelChainResponseDTO response = new TravelChainResponseDTO();
        response.getSteps().add("1. AI 需求结构化解析");
        TravelIntentDTO intentDTO = new TravelIntentDTO();
        intentDTO.setUserId(userId);
        intentDTO.setMessage(request.getMessage());
        StructuredTravelDemand demand = intentParseService.parse(intentDTO);
        if (!hasText(demand.getStartPlace())) {
            demand.setStartPlace(firstText(request.getStartPlace(), request.getCurrentLocation()));
        }
        if (!hasText(demand.getEndPlace())) {
            demand.setEndPlace(request.getEndPlace());
        }
        sceneRuleService.apply(demand);
        fillCompleteness(demand, request);
        response.setDemand(demand);

        if (!demand.getMissingFields().isEmpty()) {
            response.setComplete(false);
            response.setStage("USER_CLARIFY");
            response.setNeedUserInput(true);
            response.setMissingFields(demand.getMissingFields());
            response.setClarifyingQuestion(demand.getClarifyingQuestion());
            response.setPartialDemand(demand);
            response.getSteps().add("2. 关键信息缺失，停止后续规划并追问用户");
            return response;
        }

        response.getSteps().add("2. 跨城出行链分段构建");
        if (!hasLngLat(request.getStartLocation()) || !hasLngLat(request.getEndLocation())) {
            throw new IllegalArgumentException("起始地和终点地必须先从高德地点候选中确认经纬度，不能在地理精度未确认时规划路线");
        }
        TripSegmentBuilderService.SegmentBuildResult segmentResult = tripSegmentBuilderService.build(request, demand);
        response.setTripSegments(segmentResult.segments());
        response.setTrainProducts(segmentResult.trains());
        response.setHotelProducts(segmentResult.hotels());

        RoutePlanResponseDTO routePlan = buildRoutePlanFromSegments(request, demand, segmentResult.segments());
        response.setRoutePlan(routePlan);
        response.setGeoInfo(buildGeoInfo(routePlan));
        response.setWeatherInfo(routePlan.getEndWeather());
        List<Route> routes = routePlan.getRoutes() == null ? List.of() : routePlan.getRoutes();
        response.setRoutePlans(routes);
        response.setTrafficProducts(new java.util.ArrayList<>(segmentResult.trains()));

        if (Boolean.TRUE.equals(demand.getHotelRequired()) || Boolean.TRUE.equals(demand.getNeedHotel())) {
            response.getSteps().add("3. 酒店匹配服务调用");
            // ★ 使用标准城市名而非原始地名
            String hotelCity = TransportHubServiceImpl.normalizeCityName(
                    firstText(request.getEndCity(), demand.getEndPlace()));
            HotelSearchRequest hotelRequest = new HotelSearchRequest();
            hotelRequest.setCityName(hotelCity);
            hotelRequest.setAdcode(request.getEndAdcode());
            hotelRequest.setAdults(demand.getPeopleCount() == null ? 1 : demand.getPeopleCount());
            hotelRequest.setRooms(1);
            hotelRequest.setPreference(demand.getBudget());
            // 传递目的地经纬度，确保酒店 POI 搜索限定在目的地附近
            if (hasLngLat(request.getEndLocation())) {
                String[] parts = request.getEndLocation().split(",");
                hotelRequest.setLongitude(new BigDecimal(parts[0]));
                hotelRequest.setLatitude(new BigDecimal(parts[1]));
            }
            List<HotelItemDTO> hotels = hotelModuleService.searchHotels(hotelRequest);
            if (!segmentResult.hotels().isEmpty()) {
                hotels = segmentResult.hotels().stream().map(this::toHotelItem).toList();
            }
            response.setHotels(hotels);
            response.setHotelMatches(hotels);
        } else {
            response.getSteps().add("3. 未要求住宿，跳过酒店匹配");
        }

        response.getSteps().add("4. 风险预警规则引擎");
        List<RiskWarningDTO> risks = riskService.generateWarnings(demand, routePlan);
        response.setRisks(risks);
        response.setRiskWarnings(risks);
        response.getSteps().add("5. 全过程服务时间轴");
        List<TravelTimelineItem> timeline = timelineService.generate(userId, null, demand);
        response.setTimeline(timeline);
        response.setTimelineItems(timeline);
        response.setReminderTemplates(reminderTemplateService.listBySceneType(valueOrDefault(demand.getSceneType(), "TOURISM")));
        response.setMockOrderPreview(buildMockOrderPreview(demand, routes));
        response.getSteps().add("6. 服务报告生成");
        TravelServiceReport report = reportService.save(buildReport(userId, demand, response));
        response.setReport(report);
        response.setReportPreview(report);
        return response;
    }

    private void fillCompleteness(StructuredTravelDemand demand, TravelChainRequestDTO request) {
        demand.getMissingFields().clear();
        demand.getMissingFields().addAll(clarifyService.checkMissingFields(demand, request));
        demand.setClarifyingQuestion(demand.getMissingFields().isEmpty()
                ? null
                : clarifyService.buildQuestion(demand.getMissingFields()));
    }

    private RoutePlanResponseDTO buildRoutePlanFromSegments(TravelChainRequestDTO request,
                                                            StructuredTravelDemand demand,
                                                            List<TripSegmentDTO> segments) {
        RoutePlanResponseDTO routePlan = new RoutePlanResponseDTO();
        // ★ 标准化城市名
        String normalizedStartCity = TransportHubServiceImpl.normalizeCityName(
                firstText(request.getStartCity(), demand.getStartPlace()));
        String normalizedEndCity = TransportHubServiceImpl.normalizeCityName(
                firstText(request.getEndCity(), demand.getEndPlace()));
        routePlan.setStartLocation(locationInfo(demand.getStartPlace(), request.getStartLocation(), request.getStartAdcode(), normalizedStartCity));
        routePlan.setEndLocation(locationInfo(demand.getEndPlace(), request.getEndLocation(), request.getEndAdcode(), normalizedEndCity));
        routePlan.setWaypointLocations(List.of());
        try {
            if (hasText(request.getStartAdcode())) {
                routePlan.setStartWeather(amapWeatherService.getLiveWeather(request.getStartAdcode()));
            }
        } catch (Exception ignored) {
            routePlan.setStartWeather(null);
        }
        try {
            if (hasText(request.getEndAdcode())) {
                routePlan.setEndWeather(amapWeatherService.getLiveWeather(request.getEndAdcode()));
            }
        } catch (Exception ignored) {
            routePlan.setEndWeather(null);
        }
        routePlan.setRoutes(buildCandidateRoutes(request, demand, segments));
        routePlan.setServiceCapabilities(List.of("跨城分段出行链", "枢纽接驳", "高铁模拟预约", "天气与路线风险提醒"));
        return routePlan;
    }

    private List<Route> buildCandidateRoutes(TravelChainRequestDTO request, StructuredTravelDemand demand, List<TripSegmentDTO> segments) {
        double distance = segments.stream().mapToDouble(item -> item.getDistanceKm() == null ? 0D : item.getDistanceKm()).sum();
        int duration = segments.stream().mapToInt(item -> item.getDurationMinutes() == null ? 0 : item.getDurationMinutes()).sum();
        double cost = segments.stream()
                .map(TripSegmentDTO::getEstimatedCost)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        String summary = segments.stream().map(TripSegmentDTO::getTitle).reduce((a, b) -> a + " / " + b).orElse("");
        List<Route> routes = new java.util.ArrayList<>();
        routes.add(candidateRoute(request, demand, "SMART", "方案 A：推荐方案", distance, duration, cost, summary, "优先兼顾准时、接驳和可预约资源。"));
        routes.add(candidateRoute(request, demand, "FAST", "方案 B：快速方案", distance, Math.max(1, duration - 12), cost + 35, summary, "压缩接驳等待时间，适合商务准时到达。"));
        routes.add(candidateRoute(request, demand, "ECO", "方案 C：经济方案", distance, duration + 18, Math.max(0, cost - 28), summary, "优先控制总费用，适合预算敏感出行。"));
        return routes;
    }

    private Route candidateRoute(TravelChainRequestDTO request, StructuredTravelDemand demand, String type, String name,
                                 double distance, int duration, double cost, String summary, String reason) {
        Route route = new Route();
        route.setUserId(request.getUserId());
        route.setStartPoint(demand.getStartPlace());
        route.setEndPoint(demand.getEndPlace());
        route.setRouteType(type);
        route.setRouteName(name);
        route.setDistance(Math.round(distance * 10D) / 10D);
        route.setDuration(duration);
        route.setCost(Math.round(cost * 10D) / 10D);
        route.setDescription(summary);
        route.setRecommendReason(reason);
        route.setPreference(demand.getSortRule());
        return route;
    }

    private LocationInfo locationInfo(String name, String lngLat, String adcode, String city) {
        LocationInfo info = new LocationInfo();
        info.setName(name);
        info.setFormattedAddress(name);
        info.setLocation(lngLat);
        info.setAdcode(adcode);
        info.setCity(city);
        return info;
    }

    private HotelItemDTO toHotelItem(HotelProduct hotel) {
        HotelItemDTO item = new HotelItemDTO();
        item.setId(hotel.getId());
        item.setHotelId(String.valueOf(hotel.getId()));
        item.setName(hotel.getHotelName());
        item.setHotelName(hotel.getHotelName());
        item.setAddress(hotel.getAddress());
        item.setLatitude(hotel.getLatitude());
        item.setLongitude(hotel.getLongitude());
        item.setRating(hotel.getRating());
        item.setScore(hotel.getRating());
        item.setLowestPrice(hotel.getPrice());
        item.setMinPrice(hotel.getPrice());
        item.setTags(hotel.getTags());
        item.setDistanceText(hotel.getDistanceDesc());
        item.setDescription("来自 hotel_product 本地演示资源库，可用于模拟预约。");
        item.setSource(hotel.getDataSource());
        return item;
    }

    private TravelGeoInfoDTO buildGeoInfo(RoutePlanResponseDTO routePlan) {
        TravelGeoInfoDTO geoInfo = new TravelGeoInfoDTO();
        geoInfo.setStart(toGeoPoint(routePlan.getStartLocation()));
        geoInfo.setEnd(toGeoPoint(routePlan.getEndLocation()));
        if (routePlan.getWaypointLocations() != null) {
            geoInfo.setWaypoints(routePlan.getWaypointLocations().stream().map(this::toGeoPoint).toList());
        }
        return geoInfo;
    }

    private TravelGeoPointDTO toGeoPoint(LocationInfo locationInfo) {
        if (locationInfo == null) {
            return null;
        }
        TravelGeoPointDTO point = new TravelGeoPointDTO();
        point.setName(locationInfo.getName());
        point.setFormattedAddress(locationInfo.getFormattedAddress());
        point.setAdcode(locationInfo.getAdcode());
        point.setCity(locationInfo.getCity());
        point.setDistrict(locationInfo.getDistrict());
        String[] parts = locationInfo.getLocation() == null ? new String[0] : locationInfo.getLocation().split(",");
        if (parts.length == 2) {
            point.setLng(Double.valueOf(parts[0]));
            point.setLat(Double.valueOf(parts[1]));
        }
        return point;
    }

    private Map<String, Object> buildMockOrderPreview(StructuredTravelDemand demand, List<Route> routes) {
        Map<String, Object> preview = new LinkedHashMap<>();
        Route firstRoute = routes == null || routes.isEmpty() ? null : routes.get(0);
        preview.put("orderType", "PACKAGE");
        preview.put("title", (firstRoute == null ? "智能出行方案" : firstRoute.getRouteName()));
        preview.put("startPlace", demand.getStartPlace());
        preview.put("endPlace", demand.getEndPlace());
        preview.put("totalAmount", firstRoute == null ? 0 : firstRoute.getCost());
        preview.put("status", "WAIT_CONFIRM");
        return preview;
    }

    private TravelServiceReportSaveDTO buildReport(String userId, StructuredTravelDemand demand, TravelChainResponseDTO response) {
        TravelServiceReportSaveDTO dto = new TravelServiceReportSaveDTO();
        dto.setUserId(userId);
        dto.setTitle("全过程出行服务报告");
        dto.setStartPlace(demand.getStartPlace());
        dto.setEndPlace(demand.getEndPlace());
        dto.setSourceType("TRAVEL_CHAIN");
        dto.setReportSummary("已完成咨询、规划、匹配、风险、时间轴和报告闭环。");
        dto.setReportContent("""
                需求解析：%s
                路线数量：%d
                酒店候选：%d
                风险提醒：%d
                时间轴服务：%d
                """.formatted(
                demand,
                response.getRoutePlan() == null || response.getRoutePlan().getRoutes() == null ? 0 : response.getRoutePlan().getRoutes().size(),
                response.getHotels().size(),
                response.getRisks().size(),
                response.getTimeline().size()
        ));
        return dto;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) return value.trim();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasLngLat(String value) {
        if (!hasText(value)) {
            return false;
        }
        String[] parts = value.trim().split(",");
        if (parts.length != 2) {
            return false;
        }
        try {
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
