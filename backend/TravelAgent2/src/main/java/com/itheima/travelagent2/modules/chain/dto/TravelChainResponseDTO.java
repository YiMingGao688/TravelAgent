package com.itheima.travelagent2.modules.chain.dto;

import com.itheima.travelagent2.modules.hotel.dto.HotelItemDTO;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.product.hotel.entity.HotelProduct;
import com.itheima.travelagent2.modules.product.train.entity.TrainProduct;
import com.itheima.travelagent2.modules.reminder.entity.ReminderTemplate;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.dto.RoutePlanResponseDTO;
import com.itheima.travelagent2.modules.route.dto.WeatherInfo;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TravelChainResponseDTO {
    private Boolean complete = true;
    private String stage = "PLAN_GENERATE";
    private Boolean needUserInput = false;
    private String clarifyingQuestion;
    private List<String> missingFields = new ArrayList<>();
    private StructuredTravelDemand demand;
    private StructuredTravelDemand partialDemand;
    private TravelGeoInfoDTO geoInfo;
    private WeatherInfo weatherInfo;
    private RoutePlanResponseDTO routePlan;
    private List<Route> routePlans = new ArrayList<>();
    private List<Object> trafficProducts = new ArrayList<>();
    private List<TripSegmentDTO> tripSegments = new ArrayList<>();
    private List<TrainProduct> trainProducts = new ArrayList<>();
    private List<HotelProduct> hotelProducts = new ArrayList<>();
    private List<ReminderTemplate> reminderTemplates = new ArrayList<>();
    private List<HotelItemDTO> hotels = new ArrayList<>();
    private List<HotelItemDTO> hotelMatches = new ArrayList<>();
    private List<RiskWarningDTO> risks = new ArrayList<>();
    private List<RiskWarningDTO> riskWarnings = new ArrayList<>();
    private List<TravelTimelineItem> timeline = new ArrayList<>();
    private List<TravelTimelineItem> timelineItems = new ArrayList<>();
    private Object mockOrderPreview;
    private TravelServiceReport report;
    private TravelServiceReport reportPreview;
    private List<String> steps = new ArrayList<>();
    private Object partialResult;
}
