package com.itheima.travelagent2.modules.chain;

import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.hub.entity.TransportHub;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.dto.LocationInfo;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.dto.WeatherInfo;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import com.itheima.travelagent2.modules.chain.dto.TripSegmentDTO;
import com.itheima.travelagent2.modules.chain.dto.TravelGeoInfoDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划上下文：保存状态机各阶段产生的中间结果。
 * 所有路线/交通/酒店数据必须来自高德 API 或数据库，不允许大模型凭空生成。
 */
@Data
public class PlanningContext {
    // ---- 当前状态 ----
    private PlanningState currentState = PlanningState.IDLE;
    private String status = "pending";           // pending | running | done | need_input | error
    private String nextQuestion;                  // 需要用户回答的问题
    private final List<String> toolCalls = new ArrayList<>();  // 本阶段调用的工具日志
    private final List<String> steps = new ArrayList<>();

    // ---- 用户输入 ----
    private String userId;
    private String sessionId;
    private String rawMessage;
    private String startPlaceInput;
    private String endPlaceInput;
    private String startCity;
    private String endCity;
    private String startAdcode;
    private String endAdcode;
    private String startLocation;   // "lng,lat"
    private String endLocation;     // "lng,lat"

    // ---- 需求解析 ----
    private StructuredTravelDemand demand;
    private boolean complete = true;
    private boolean needUserInput;
    private List<String> missingFields = new ArrayList<>();

    // ---- 地点解析 ----
    private LocationInfo resolvedStartLocation;
    private LocationInfo resolvedEndLocation;
    private List<LocationInfo> locationCandidates = new ArrayList<>();

    // ---- 交通枢纽 ----
    private TransportHub startHub;
    private TransportHub endHub;

    // ---- 出行链分段 ----
    private List<TripSegmentDTO> tripSegments = new ArrayList<>();

    // ---- 交通产品 ----
    private List<TrainProduct> trainProducts = new ArrayList<>();
    private List<Object> trafficProducts = new ArrayList<>();

    // ---- 路线方案 ----
    private List<Route> routePlans = new ArrayList<>();
    private RoutePlanResponseDTO routePlan;
    private TravelGeoInfoDTO geoInfo;
    private WeatherInfo weatherInfo;

    // ---- 酒店 ----
    private List<HotelItemDTO> hotels = new ArrayList<>();
    private List<HotelProduct> hotelProducts = new ArrayList<>();

    // ---- 风险和提醒 ----
    private List<RiskWarningDTO> risks = new ArrayList<>();
    private List<ReminderTemplate> reminderTemplates = new ArrayList<>();
    private List<TravelTimelineItem> timeline = new ArrayList<>();

    // ---- 确认与订单 ----
    private boolean userConfirmed;
    private Object mockOrderPreview;
    private TravelServiceReport report;

    // ---- 前端展示 ----
    private Map<String, Object> partialResult = new LinkedHashMap<>();

    public void logToolCall(String tool) {
        toolCalls.add(tool);
    }

    public void logStep(String step) {
        steps.add(step);
    }

    public PlanningContext transitionTo(PlanningState state) {
        this.currentState = state;
        this.status = "running";
        return this;
    }

    public void markDone() {
        this.status = "done";
    }

    public void markNeedInput(String question) {
        this.status = "need_input";
        this.needUserInput = true;
        this.nextQuestion = question;
    }
}
