package com.itheima.travelagent2.modules.intent.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StructuredTravelDemand {
    private String intentType;
    private String startPlace;
    private String endPlace;
    private List<String> waypoints = new ArrayList<>();
    private String departureDate;
    private String departureTime;
    private String transportPreference;
    private Boolean hotelRequired;
    private Boolean needHotel;
    private Integer peopleCount;
    private String budget;
    private String sceneType;
    private String peopleType;
    private String luggageInfo;
    private String sortRule;
    private Integer days;
    private List<String> reminderItems = new ArrayList<>();
    private List<String> ambiguousPlaces = new ArrayList<>();
    private List<String> geoCandidates = new ArrayList<>();
    private Boolean needOrder;
    private Boolean needReport;
    private List<String> missingFields = new ArrayList<>();
    private String clarifyingQuestion;
}
