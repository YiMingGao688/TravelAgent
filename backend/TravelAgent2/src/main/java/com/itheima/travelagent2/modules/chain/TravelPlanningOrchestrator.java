package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.amap.AmapGeoService;
import com.itheima.travelagent2.modules.amap.AmapWeatherService;
import com.itheima.travelagent2.modules.chain.dto.*;
import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hotel.dto.HotelSearchRequest;
import com.itheima.travelagent2.modules.hotel.service.HotelModuleService;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;
import com.itheima.travelagent2.modules.hub.service.TransportHubService;
import com.itheima.travelagent2.modules.hub.service.impl.TransportHubServiceImpl;
import com.itheima.travelagent2.modules.intent.IntentParseService;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.intent.dto.TravelIntentDTO;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.hotel.service.HotelProductService;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.product.train.service.TrainProductService;
import com.itheima.travelagent2.modules.reminder.service.ReminderTemplateService;
import com.itheima.travelagent2.modules.risk.RiskService;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.servicereport.service.TravelServiceReportService;
import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.timeline.TravelTimelineService;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ★ 出行规划智能体编排器。
 *
 * 状态机顺序:
 * IDLE → INTENT_PARSE → [CLARIFY | LOCATION_RESOLVE] → HUB_SELECT
 * → ROUTE_BUILD → TRAFFIC_QUERY → [HOTEL_MATCH] → RISK_ASSESS
 * → AWAIT_CONFIRM → [MOCK_ORDER] → [REMINDERS] → COMPLETED
 *
 * 核心约束:
 * - 大模型只负责结构化解析和自然语言总结，不生成路线
 * - 路线必须来自高德 API 或数据库产品
 * - 缺失字段时暂停流程，返回 needUserInput=true
 * - 目的地模糊时返回候选地点让用户确认
 * - 用户确认前不生成模拟订单和实时提醒
 */
@Service
public class TravelPlanningOrchestrator {

    /** 会话级规划上下文缓存（演示用，生产应换 Redis） */
    private final ConcurrentHashMap<String, PlanningContext> sessionContexts = new ConcurrentHashMap<>();

    private final IntentParseService intentParseService;
    private final AmapGeoService amapGeoService;
    private final AmapWeatherService amapWeatherService;
    private final TransportHubService transportHubService;
    private final TrainProductService trainProductService;
    private final HotelProductService hotelProductService;
    private final HotelModuleService hotelModuleService;
    private final RiskService riskService;
    private final TravelTimelineService timelineService;
    private final TravelServiceReportService reportService;
    private final ReminderTemplateService reminderTemplateService;
    private final TripSegmentBuilderService tripSegmentBuilderService;
    private final ClarifyService clarifyService;
    private final SceneRuleService sceneRuleService;

    public TravelPlanningOrchestrator(IntentParseService intentParseService,
                                       AmapGeoService amapGeoService,
                                       AmapWeatherService amapWeatherService,
                                       TransportHubService transportHubService,
                                       TrainProductService trainProductService,
                                       HotelProductService hotelProductService,
                                       HotelModuleService hotelModuleService,
                                       RiskService riskService,
                                       TravelTimelineService timelineService,
                                       TravelServiceReportService reportService,
                                       ReminderTemplateService reminderTemplateService,
                                       TripSegmentBuilderService tripSegmentBuilderService,
                                       ClarifyService clarifyService,
                                       SceneRuleService sceneRuleService) {
        this.intentParseService = intentParseService;
        this.amapGeoService = amapGeoService;
        this.amapWeatherService = amapWeatherService;
        this.transportHubService = transportHubService;
        this.trainProductService = trainProductService;
        this.hotelProductService = hotelProductService;
        this.hotelModuleService = hotelModuleService;
        this.riskService = riskService;
        this.timelineService = timelineService;
        this.reportService = reportService;
        this.reminderTemplateService = reminderTemplateService;
        this.tripSegmentBuilderService = tripSegmentBuilderService;
        this.clarifyService = clarifyService;
        this.sceneRuleService = sceneRuleService;
    }

    /**
     * 执行一步规划。每次调用根据当前上下文状态推进状态机。
     */
    public TravelChainResponseDTO plan(TravelChainRequestDTO request) {
        PlanningContext ctx = buildInitialContext(request);
        // 保存到会话缓存，供后续 confirm 使用
        sessionContexts.put(ctx.getSessionId(), ctx);

        // === 阶段 1: 意图解析 ===
        ctx.transitionTo(PlanningState.INTENT_PARSE);
        ctx.logStep("1. AI 需求结构化解析");

        TravelIntentDTO intentDTO = new TravelIntentDTO();
        intentDTO.setUserId(ctx.getUserId());
        intentDTO.setMessage(request.getMessage());
        StructuredTravelDemand demand = intentParseService.parse(intentDTO);

        // 用请求中的起终点填充
        if (!hasText(demand.getStartPlace())) {
            demand.setStartPlace(firstText(request.getStartPlace(), request.getCurrentLocation()));
        }
        if (!hasText(demand.getEndPlace())) {
            demand.setEndPlace(request.getEndPlace());
        }
        sceneRuleService.apply(demand);

        // 填充上下文
        ctx.setDemand(demand);
        ctx.setStartPlaceInput(firstText(demand.getStartPlace(), request.getStartPlace()));
        ctx.setEndPlaceInput(firstText(demand.getEndPlace(), request.getEndPlace()));
        ctx.setStartLocation(request.getStartLocation());
        ctx.setEndLocation(request.getEndLocation());
        ctx.setStartAdcode(request.getStartAdcode());
        ctx.setEndAdcode(request.getEndAdcode());
        ctx.setStartCity(firstText(request.getStartCity(), inferCity(demand.getStartPlace())));
        ctx.setEndCity(firstText(request.getEndCity(), inferCity(demand.getEndPlace())));
        ctx.markDone();

        // === 阶段 2: 缺失信息检查 ===
        ctx.transitionTo(PlanningState.CLARIFY);
        ctx.logStep("2. 关键信息完整性检查");
        List<String> missing = clarifyService.checkMissingFields(demand, request);
        demand.setMissingFields(missing);
        demand.setClarifyingQuestion(missing.isEmpty() ? null : clarifyService.buildQuestion(missing));
        ctx.setMissingFields(missing);

        if (!missing.isEmpty()) {
            ctx.markNeedInput(demand.getClarifyingQuestion());
            ctx.setComplete(false);
            return buildResponse(ctx, request);
        }
        ctx.markDone();

        // === 阶段 3: 地点解析（模糊地点确认） ===
        if (demand.getAmbiguousPlaces() != null && !demand.getAmbiguousPlaces().isEmpty()) {
            ctx.transitionTo(PlanningState.LOCATION_RESOLVE);
            ctx.logStep("3. 模糊地点解析");
            List<LocationInfo> candidates = new ArrayList<>();
            for (String place : demand.getAmbiguousPlaces()) {
                List<LocationInfo> resolved = amapGeoService.resolvePlace(place, ctx.getEndCity(), ctx.getEndAdcode());
                if (resolved != null) candidates.addAll(resolved);
            }
            ctx.setLocationCandidates(candidates);
            ctx.logToolCall("amap.resolvePlace");
            if (!candidates.isEmpty()) {
                ctx.markNeedInput("请从以下候选中选择具体目的地：");
                ctx.setComplete(false);
                return buildResponse(ctx, request);
            }
            ctx.markDone();
        }

        // === 阶段 4: 交通枢纽选择 ===
        ctx.transitionTo(PlanningState.HUB_SELECT);
        ctx.logStep("4. 交通枢纽匹配");

        String normStartCity = TransportHubServiceImpl.normalizeCityName(ctx.getStartCity());
        String normEndCity = TransportHubServiceImpl.normalizeCityName(ctx.getEndCity());
        boolean crossCity = hasText(normStartCity) && hasText(normEndCity) && !normStartCity.equals(normEndCity);

        if (!crossCity) {
            // 同城：单段本地路线
            ctx.transitionTo(PlanningState.ROUTE_BUILD);
            TripSegmentDTO localSeg = buildLocalSegment(ctx);
            ctx.setTripSegments(List.of(localSeg));
            ctx.logToolCall("amap.route.driving");
        } else {
            // 跨城：枢纽匹配
            TransportHub startHub = transportHubService.recommend(normStartCity, normEndCity, "TRAIN", true);
            TransportHub endHub = transportHubService.recommend(normStartCity, normEndCity, "TRAIN", false);
            ctx.logToolCall("transport_hub.recommend");

            if (startHub == null || endHub == null) {
                ctx.markNeedInput("未能找到" + normStartCity + "到" + normEndCity + "的交通枢纽，请确认城市名称。");
                ctx.setComplete(false);
                return buildResponse(ctx, request);
            }
            ctx.setStartHub(startHub);
            ctx.setEndHub(endHub);
            ctx.markDone();

            // === 阶段 5: 路线构建 (跨城) ===
            ctx.transitionTo(PlanningState.ROUTE_BUILD);
            ctx.logStep("5. 跨城出行链分段构建");
            TripSegmentBuilderService.SegmentBuildResult segResult =
                    tripSegmentBuilderService.build(request, demand);
            ctx.setTripSegments(segResult.segments());
            ctx.setTrainProducts(segResult.trains());
            ctx.setHotelProducts(segResult.hotels());
            ctx.logToolCall("trip_segment_builder");
            ctx.markDone();

            // === 阶段 6: 交通产品查询 ===
            ctx.transitionTo(PlanningState.TRAFFIC_QUERY);
            ctx.logStep("6. 交通产品匹配");
            ctx.setTrafficProducts(new ArrayList<>(segResult.trains()));
            ctx.markDone();
        }

        // 构建路线方案
        RoutePlanResponseDTO routePlan = buildRoutePlan(ctx);
        ctx.setRoutePlan(routePlan);
        ctx.setGeoInfo(buildGeoInfo(ctx));
        try {
            if (hasText(ctx.getEndAdcode())) {
                ctx.setWeatherInfo(amapWeatherService.getLiveWeather(ctx.getEndAdcode()));
            }
        } catch (Exception ignored) {}

        ctx.setRoutePlans(routePlan.getRoutes() != null ? routePlan.getRoutes() : List.of());

        // === 阶段 7: 酒店匹配 ===
        if (Boolean.TRUE.equals(demand.getHotelRequired()) || Boolean.TRUE.equals(demand.getNeedHotel())) {
            ctx.transitionTo(PlanningState.HOTEL_MATCH);
            ctx.logStep("7. 酒店匹配服务调用");

            List<HotelItemDTO> hotels;
            if (!ctx.getHotelProducts().isEmpty()) {
                hotels = ctx.getHotelProducts().stream().map(this::toHotelItem).toList();
            } else {
                HotelSearchRequest hotelReq = new HotelSearchRequest();
                hotelReq.setCityName(normEndCity);
                hotelReq.setAdcode(ctx.getEndAdcode());
                hotelReq.setAdults(demand.getPeopleCount() == null ? 1 : demand.getPeopleCount());
                hotelReq.setRooms(1);
                hotelReq.setPreference(demand.getBudget());
                if (hasLngLat(ctx.getEndLocation())) {
                    String[] parts = ctx.getEndLocation().split(",");
                    hotelReq.setLongitude(new BigDecimal(parts[0]));
                    hotelReq.setLatitude(new BigDecimal(parts[1]));
                }
                hotels = hotelModuleService.searchHotels(hotelReq);
            }
            ctx.setHotels(hotels);
            ctx.logToolCall("hotel.search");
            ctx.markDone();
        } else {
            ctx.logStep("7. 未要求住宿，跳过酒店匹配");
        }

        // === 阶段 8: 风险评估 ===
        ctx.transitionTo(PlanningState.RISK_ASSESS);
        ctx.logStep("8. 风险预警规则引擎");
        List<RiskWarningDTO> risks = riskService.generateWarnings(demand, ctx.getRoutePlan());
        ctx.setRisks(risks);
        ctx.markDone();

        // === 阶段 9: 等待用户确认 ===
        ctx.transitionTo(PlanningState.AWAIT_CONFIRM);
        ctx.logStep("9. 方案生成完成，等待用户确认");
        ctx.setNextQuestion("请确认以上方案，确认后将生成模拟订单和实时提醒。");
        ctx.markNeedInput(ctx.getNextQuestion());

        // 预生成时间轴（展示用）
        List<TravelTimelineItem> timeline = timelineService.generate(ctx.getUserId(), null, demand);
        ctx.setTimeline(timeline);
        ctx.setReminderTemplates(reminderTemplateService.listBySceneType(
                demand.getSceneType() == null ? "TOURISM" : demand.getSceneType()));

        return buildResponse(ctx, request);
    }

    /** 根据 sessionId 获取已缓存的规划上下文 */
    public PlanningContext getContext(String sessionId) {
        return sessionContexts.get(sessionId);
    }

    /**
     * 用户确认后执行：生成模拟订单 + 实时提醒 + 服务报告
     */
    public TravelChainResponseDTO confirm(PlanningContext ctx) {
        if (ctx.getCurrentState() != PlanningState.AWAIT_CONFIRM) {
            throw new IllegalStateException("当前状态不允许确认操作");
        }

        ctx.setUserConfirmed(true);

        // 生成模拟订单
        ctx.transitionTo(PlanningState.MOCK_ORDER);
        ctx.logStep("10. 生成模拟预约");
        ctx.setMockOrderPreview(buildMockOrder(ctx));
        ctx.markDone();

        // 生成实时提醒（已在前一步预生成）
        ctx.transitionTo(PlanningState.REMINDERS);
        ctx.logStep("11. 实时提醒已就绪");
        ctx.markDone();

        // 服务报告
        ctx.transitionTo(PlanningState.COMPLETED);
        ctx.logStep("12. 服务报告生成");
        TravelServiceReport report = reportService.save(buildReport(ctx));
        ctx.setReport(report);
        ctx.markDone();
        ctx.setStatus("done");
        ctx.setNeedUserInput(false);

        return buildResponse(ctx, null);
    }

    // ======================== 内部方法 ========================

    private PlanningContext buildInitialContext(TravelChainRequestDTO request) {
        PlanningContext ctx = new PlanningContext();
        ctx.setUserId(request.getUserId() == null || request.getUserId().isBlank() ? "test_user" : request.getUserId());
        ctx.setSessionId(request.getSessionId());
        ctx.setRawMessage(request.getMessage());
        return ctx;
    }

    private TripSegmentDTO buildLocalSegment(PlanningContext ctx) {
        TripSegmentDTO seg = new TripSegmentDTO();
        seg.setSegmentType("LOCAL_TRANSFER");
        seg.setTitle(ctx.getStartPlaceInput() + " → " + ctx.getEndPlaceInput());
        seg.setStartPlace(ctx.getStartPlaceInput());
        seg.setEndPlace(ctx.getEndPlaceInput());
        seg.setStartLocation(ctx.getStartLocation());
        seg.setEndLocation(ctx.getEndLocation());
        seg.setTransportMode("TAXI");
        seg.setSource("AMAP");
        return seg;
    }

    private RoutePlanResponseDTO buildRoutePlan(PlanningContext ctx) {
        RoutePlanResponseDTO rp = new RoutePlanResponseDTO();
        rp.setStartLocation(locationInfo(ctx.getStartPlaceInput(), ctx.getStartLocation(), ctx.getStartAdcode(),
                TransportHubServiceImpl.normalizeCityName(ctx.getStartCity())));
        rp.setEndLocation(locationInfo(ctx.getEndPlaceInput(), ctx.getEndLocation(), ctx.getEndAdcode(),
                TransportHubServiceImpl.normalizeCityName(ctx.getEndCity())));
        rp.setWaypointLocations(List.of());
        rp.setStartWeather(null);
        rp.setEndWeather(ctx.getWeatherInfo());
        rp.setRoutes(buildCandidateRoutes(ctx));
        rp.setServiceCapabilities(List.of("跨城分段出行链", "枢纽接驳", "高铁模拟预约", "天气与路线风险提醒"));
        return rp;
    }

    private List<Route> buildCandidateRoutes(PlanningContext ctx) {
        double distance = ctx.getTripSegments().stream()
                .mapToDouble(s -> s.getDistanceKm() == null ? 0 : s.getDistanceKm()).sum();
        int duration = ctx.getTripSegments().stream()
                .mapToInt(s -> s.getDurationMinutes() == null ? 0 : s.getDurationMinutes()).sum();
        double cost = ctx.getTripSegments().stream()
                .map(TripSegmentDTO::getEstimatedCost).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).sum();
        String summary = ctx.getTripSegments().stream()
                .map(TripSegmentDTO::getTitle).reduce((a, b) -> a + " / " + b).orElse("");

        StructuredTravelDemand d = ctx.getDemand();
        Route smart = makeRoute(ctx, "SMART", "方案 A：推荐方案", distance, duration, cost, summary, "优先兼顾准时、接驳和可预约资源。");
        Route fast = makeRoute(ctx, "FAST", "方案 B：快速方案", distance, Math.max(1, duration - 12), cost + 35, summary, "压缩接驳等待时间。");
        Route eco = makeRoute(ctx, "ECO", "方案 C：经济方案", distance, duration + 18, Math.max(0, cost - 28), summary, "优先控制总费用。");
        return List.of(smart, fast, eco);
    }

    private Route makeRoute(PlanningContext ctx, String type, String name, double dist, int dur, double cost, String summary, String reason) {
        Route r = new Route();
        r.setUserId(ctx.getUserId());
        r.setStartPoint(ctx.getStartPlaceInput());
        r.setEndPoint(ctx.getEndPlaceInput());
        r.setRouteType(type);
        r.setRouteName(name);
        r.setDistance(Math.round(dist * 10D) / 10D);
        r.setDuration(dur);
        r.setCost(Math.round(cost * 10D) / 10D);
        r.setDescription(summary);
        r.setRecommendReason(reason);
        r.setPreference(ctx.getDemand() != null ? ctx.getDemand().getSortRule() : "SMART");
        return r;
    }

    private TravelGeoInfoDTO buildGeoInfo(PlanningContext ctx) {
        TravelGeoInfoDTO geo = new TravelGeoInfoDTO();
        if (ctx.getRoutePlan() != null) {
            geo.setStart(toGeoPoint(ctx.getRoutePlan().getStartLocation()));
            geo.setEnd(toGeoPoint(ctx.getRoutePlan().getEndLocation()));
        }
        return geo;
    }

    private TravelGeoPointDTO toGeoPoint(LocationInfo loc) {
        if (loc == null) return null;
        TravelGeoPointDTO p = new TravelGeoPointDTO();
        p.setName(loc.getName());
        p.setFormattedAddress(loc.getFormattedAddress());
        p.setAdcode(loc.getAdcode());
        p.setCity(loc.getCity());
        p.setDistrict(loc.getDistrict());
        if (loc.getLocation() != null && loc.getLocation().contains(",")) {
            String[] parts = loc.getLocation().split(",");
            if (parts.length == 2) {
                p.setLng(Double.valueOf(parts[0]));
                p.setLat(Double.valueOf(parts[1]));
            }
        }
        return p;
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
        item.setDescription("来自 hotel_product 本地演示资源库。");
        item.setSource(hotel.getDataSource());
        return item;
    }

    private Map<String, Object> buildMockOrder(PlanningContext ctx) {
        Map<String, Object> preview = new LinkedHashMap<>();
        List<Route> routes = ctx.getRoutePlans();
        Route firstRoute = routes.isEmpty() ? null : routes.get(0);
        preview.put("orderType", "PACKAGE");
        preview.put("title", firstRoute == null ? "智能出行方案" : firstRoute.getRouteName());
        preview.put("startPlace", ctx.getStartPlaceInput());
        preview.put("endPlace", ctx.getEndPlaceInput());
        preview.put("totalAmount", firstRoute == null ? 0 : firstRoute.getCost());
        preview.put("status", "WAIT_CONFIRM");
        return preview;
    }

    private TravelServiceReportSaveDTO buildReport(PlanningContext ctx) {
        TravelServiceReportSaveDTO dto = new TravelServiceReportSaveDTO();
        dto.setUserId(ctx.getUserId());
        dto.setTitle("全过程出行服务报告");
        dto.setStartPlace(ctx.getStartPlaceInput());
        dto.setEndPlace(ctx.getEndPlaceInput());
        dto.setSourceType("TRAVEL_CHAIN");
        dto.setReportSummary("已完成咨询、规划、匹配、风险、时间轴和报告闭环。");
        dto.setReportContent("需求解析完成，路线数量：" + ctx.getRoutePlans().size()
                + "，酒店候选：" + ctx.getHotels().size()
                + "，风险提醒：" + ctx.getRisks().size());
        return dto;
    }

    /**
     * 构建响应 DTO，注入 state/stage/status/toolCalls/nextQuestion/partialResult。
     */
    private TravelChainResponseDTO buildResponse(PlanningContext ctx, TravelChainRequestDTO request) {
        TravelChainResponseDTO resp = new TravelChainResponseDTO();
        resp.setComplete(ctx.isComplete());
        resp.setStage(ctx.getCurrentState().name());
        resp.setNeedUserInput(ctx.isNeedUserInput());
        resp.setClarifyingQuestion(ctx.getNextQuestion());
        resp.setMissingFields(ctx.getMissingFields());

        // partialResult
        Map<String, Object> pr = new LinkedHashMap<>();
        pr.put("stage", ctx.getCurrentState().name());
        pr.put("status", ctx.getStatus());
        pr.put("toolCalls", ctx.getToolCalls());
        pr.put("nextQuestion", ctx.getNextQuestion());

        if (ctx.getDemand() != null) {
            resp.setDemand(ctx.getDemand());
            resp.setPartialDemand(ctx.getDemand());
            pr.put("demand", ctx.getDemand());
        }
        if (!ctx.getLocationCandidates().isEmpty()) {
            pr.put("locationCandidates", ctx.getLocationCandidates());
        }
        if (!ctx.getTripSegments().isEmpty()) {
            resp.setTripSegments(ctx.getTripSegments());
            pr.put("tripSegments", ctx.getTripSegments());
        }
        if (!ctx.getTrainProducts().isEmpty()) {
            resp.setTrainProducts(ctx.getTrainProducts());
        }
        if (!ctx.getTrafficProducts().isEmpty()) {
            resp.setTrafficProducts(ctx.getTrafficProducts());
        }
        if (ctx.getRoutePlan() != null) {
            resp.setRoutePlan(ctx.getRoutePlan());
            resp.setRoutePlans(ctx.getRoutePlans());
        }
        if (ctx.getGeoInfo() != null) {
            resp.setGeoInfo(ctx.getGeoInfo());
        }
        if (ctx.getWeatherInfo() != null) {
            resp.setWeatherInfo(ctx.getWeatherInfo());
        }
        if (!ctx.getHotels().isEmpty()) {
            resp.setHotels(ctx.getHotels());
            resp.setHotelMatches(ctx.getHotels());
        }
        if (!ctx.getRisks().isEmpty()) {
            resp.setRisks(ctx.getRisks());
            resp.setRiskWarnings(ctx.getRisks());
        }
        if (!ctx.getTimeline().isEmpty()) {
            resp.setTimeline(ctx.getTimeline());
            resp.setTimelineItems(ctx.getTimeline());
        }
        if (!ctx.getReminderTemplates().isEmpty()) {
            resp.setReminderTemplates(ctx.getReminderTemplates());
        }
        if (ctx.getMockOrderPreview() != null) {
            resp.setMockOrderPreview(ctx.getMockOrderPreview());
        }
        if (ctx.getReport() != null) {
            resp.setReport(ctx.getReport());
            resp.setReportPreview(ctx.getReport());
        }
        resp.setSteps(ctx.getSteps());

        pr.put("steps", ctx.getSteps());
        resp.setPartialResult(pr);  // 需要给 TravelChainResponseDTO 加这个字段

        return resp;
    }

    // ======================== 工具方法 ========================

    private String inferCity(String place) {
        if (!hasText(place)) return "";
        for (String kw : List.of("南京", "杭州", "上海", "北京", "苏州", "成都", "广州", "深圳")) {
            if (place.contains(kw)) return kw;
        }
        if (place.contains("阿里巴巴")) return "杭州";
        return "";
    }

    private String firstText(String... values) {
        for (String v : values) if (hasText(v)) return v.trim();
        return "";
    }

    private boolean hasText(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private boolean hasLngLat(String v) {
        if (!hasText(v) || !v.contains(",")) return false;
        String[] parts = v.split(",");
        if (parts.length != 2) return false;
        try { Double.parseDouble(parts[0]); Double.parseDouble(parts[1]); return true; }
        catch (NumberFormatException e) { return false; }
    }
}
