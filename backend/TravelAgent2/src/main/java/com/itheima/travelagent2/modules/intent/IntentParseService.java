package com.itheima.travelagent2.modules.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.intent.dto.TravelIntentDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentParseService {
    private final ZhipuAiClient zhipuAiClient;
    private final ObjectMapper objectMapper;

    public IntentParseService(ZhipuAiClient zhipuAiClient, ObjectMapper objectMapper) {
        this.zhipuAiClient = zhipuAiClient;
        this.objectMapper = objectMapper;
    }

    public StructuredTravelDemand parse(TravelIntentDTO dto) {
        String message = dto == null || dto.getMessage() == null ? "" : dto.getMessage().trim();
        if (message.isBlank()) {
            return new StructuredTravelDemand();
        }
        StructuredTravelDemand fallback = parseByRules(message);
        try {
            String json = zhipuAiClient.chat(systemPrompt(), "用户输入：" + message);
            StructuredTravelDemand aiDemand = objectMapper.readValue(extractJson(json), StructuredTravelDemand.class);
            mergeMissing(aiDemand, fallback);
            return aiDemand;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private StructuredTravelDemand parseByRules(String message) {
        StructuredTravelDemand demand = new StructuredTravelDemand();
        demand.setIntentType(containsAny(message, "退", "改签", "投诉", "行李", "延误") ? "AFTER_SALE" : "TRAVEL_PLAN");
        demand.setStartPlace(extractStart(message));
        demand.setEndPlace(extractEnd(message));
        demand.setWaypoints(extractWaypoints(message));
        demand.setDepartureDate(extractDate(message));
        demand.setDepartureTime(extractTime(message));
        demand.setTransportPreference(detectTransport(message));
        demand.setHotelRequired(containsAny(message, "酒店", "住宿", "住一晚", "房间"));
        demand.setNeedHotel(demand.getHotelRequired());
        demand.setPeopleCount(extractPeopleCount(message));
        demand.setDays(extractDays(message));
        demand.setBudget(detectBudget(message));
        demand.setSceneType(detectScene(message));
        demand.setReminderItems(detectReminderItems(message));
        demand.setAmbiguousPlaces(detectAmbiguousPlaces(message, demand.getEndPlace()));
        demand.setPeopleType(detectPeopleType(message));
        demand.setLuggageInfo(containsAny(message, "行李", "箱子", "托运") ? "有行李" : null);
        demand.setSortRule(detectSortRule(message));
        demand.setNeedOrder(containsAny(message, "订", "买", "预订", "下单", "预约"));
        demand.setNeedReport(containsAny(message, "报告", "总结", "服务报告"));
        applyBusinessTripHotelRule(message, demand);
        fillMissingFields(demand);
        return demand;
    }

    private String systemPrompt() {
        return """
                你是出行需求结构化解析器，只输出严格 JSON，不要输出解释、Markdown 或代码块。
                字段固定为：intentType,startPlace,endPlace,waypoints,departureDate,departureTime,transportPreference,hotelRequired,needHotel,peopleCount,budget,sceneType,peopleType,luggageInfo,sortRule,days,reminderItems,ambiguousPlaces,needOrder,needReport,missingFields,clarifyingQuestion。
                如果用户说“从A出发去B，下午再去C”，A 是 startPlace，C 是 endPlace，B 放入 waypoints。
                “少步行”必须解析为 transportPreference=MIN_WALK。
                缺失字段返回 null；布尔字段无法判断时返回 false；peopleCount 缺失时返回 1。
                商务出差必须输出 sceneType=BUSINESS_TRIP；出差两天且用户未明确不住酒店时 needHotel 返回 null。
                如果缺少 startPlace 或 endPlace，把缺失字段放入 missingFields，并给出 clarifyingQuestion。
                不要编造用户没有提供的信息。
                """;
    }

    private void mergeMissing(StructuredTravelDemand target, StructuredTravelDemand fallback) {
        if (target.getIntentType() == null) target.setIntentType(fallback.getIntentType());
        if (target.getStartPlace() == null) target.setStartPlace(fallback.getStartPlace());
        if (target.getEndPlace() == null) target.setEndPlace(fallback.getEndPlace());
        if (target.getWaypoints() == null || target.getWaypoints().isEmpty()) target.setWaypoints(fallback.getWaypoints());
        if (fallback.getDepartureDate() != null) target.setDepartureDate(fallback.getDepartureDate());
        if (fallback.getDepartureTime() != null) target.setDepartureTime(fallback.getDepartureTime());
        if (target.getTransportPreference() == null) target.setTransportPreference(fallback.getTransportPreference());
        if (target.getHotelRequired() == null) target.setHotelRequired(fallback.getHotelRequired());
        if (target.getNeedHotel() == null) target.setNeedHotel(fallback.getNeedHotel());
        if (target.getPeopleCount() == null) target.setPeopleCount(fallback.getPeopleCount());
        if (target.getBudget() == null) target.setBudget(fallback.getBudget());
        if (target.getSceneType() == null) target.setSceneType(fallback.getSceneType());
        if (target.getPeopleType() == null) target.setPeopleType(fallback.getPeopleType());
        if (target.getLuggageInfo() == null) target.setLuggageInfo(fallback.getLuggageInfo());
        if (target.getSortRule() == null) target.setSortRule(fallback.getSortRule());
        if (target.getDays() == null) target.setDays(fallback.getDays());
        if (target.getReminderItems() == null || target.getReminderItems().isEmpty()) target.setReminderItems(fallback.getReminderItems());
        if (target.getAmbiguousPlaces() == null || target.getAmbiguousPlaces().isEmpty()) target.setAmbiguousPlaces(fallback.getAmbiguousPlaces());
        if (target.getNeedOrder() == null) target.setNeedOrder(fallback.getNeedOrder());
        if (target.getNeedReport() == null) target.setNeedReport(fallback.getNeedReport());
        fillMissingFields(target);
    }

    private String extractJson(String text) {
        int start = text == null ? -1 : text.indexOf('{');
        int end = text == null ? -1 : text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "{}";
    }

    private String extractStart(String message) {
        Matcher matcher = Pattern.compile("从([^到去出发]+)(?:到|去)").matcher(message);
        if (matcher.find()) return matcher.group(1).trim();
        return null;
    }

    private String extractEnd(String message) {
        Matcher matcher = Pattern.compile("(?:到|去)([^，。,.\\s]+)").matcher(message);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        if (last != null && !isNotPlace(last)) return normalizeCity(last);
        for (String city : List.of("杭州", "南京", "上海", "北京", "苏州", "成都", "广州", "深圳")) {
            if (message.contains(city)) return city;
        }
        return null;
    }

    /**
     * 非地名关键词，正则可能错误匹配为目的地（如"去出差"→"出差"）。
     */
    private boolean isNotPlace(String word) {
        return containsAny(word, "出差", "旅游", "旅行", "开会", "办事", "玩", "逛逛", "预订", "预定");
    }

    private List<String> extractWaypoints(String message) {
        java.util.ArrayList<String> places = new java.util.ArrayList<>();
        Matcher matcher = Pattern.compile("(?:到|去|前往)([^，。,.\\s]+)").matcher(message);
        while (matcher.find()) {
            String place = matcher.group(1);
            if (!isNotPlace(place)) places.add(normalizeCity(place));
        }
        if (places.size() <= 1) {
            return java.util.Collections.emptyList();
        }
        return places.subList(0, places.size() - 1);
    }

    private String extractDate(String message) {
        if (message.contains("明天")) return "明天";
        if (message.contains("今天")) return "今天";
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}月\\d{1,2}[日号])").matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractTime(String message) {
        Matcher matcher = Pattern.compile("(\\d{1,2}:\\d{2}|上午|下午|晚上|早上|中午)").matcher(message);
        if (matcher.find()) return matcher.group(1);
        // 未明确指定时间但包含出行意图时，给合理默认值，避免 ClarifyService 追问出发时间
        if (containsAny(message, "早上", "上午")) return "09:00";
        if (containsAny(message, "中午")) return "12:00";
        if (containsAny(message, "下午")) return "14:00";
        if (containsAny(message, "晚上")) return "18:00";
        // 用户表达了出行规划意图但未指定具体时间段，默认上午出发
        if (containsAny(message, "出发", "出差", "旅游", "旅行")) return "09:00";
        return null;
    }

    private Integer extractPeopleCount(String message) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(人|位|个)").matcher(message);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : 1;
    }

    private Integer extractDays(String message) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(天|日)").matcher(message);
        if (matcher.find()) return Integer.valueOf(matcher.group(1));
        if (containsAny(message, "两天", "二天", "2天")) return 2;
        if (containsAny(message, "三天", "3天")) return 3;
        if (containsAny(message, "一天", "1天", "当天", "当日")) return 1;
        return null;
    }

    private String detectTransport(String message) {
        if (containsAny(message, "少步行", "少走路", "步行少")) return "MIN_WALK";
        if (containsAny(message, "飞机", "机票", "航班")) return "FLIGHT";
        if (containsAny(message, "高铁", "火车", "动车")) return "TRAIN";
        if (containsAny(message, "公交", "大巴")) return "BUS";
        if (containsAny(message, "地铁")) return "METRO";
        if (containsAny(message, "打车", "用车", "网约车", "自驾")) return "CAR";
        return null;
    }

    private String detectBudget(String message) {
        if (containsAny(message, "便宜", "低价", "省钱")) return "ECONOMY";
        if (containsAny(message, "舒适", "方便")) return "COMFORT";
        if (containsAny(message, "商务", "高端")) return "BUSINESS";
        return null;
    }

    private String detectScene(String message) {
        if (containsAny(message, "下午再去", "半天", "短途")) return "SHORT_TRIP";
        if (containsAny(message, "通勤")) return "COMMUTE";
        if (containsAny(message, "出差", "开会", "客户", "公司", "总部", "园区", "商务", "会议", "拜访")) return "BUSINESS_TRIP";
        if (containsAny(message, "旅游", "旅行", "游玩")) return "TOURISM";
        if (containsAny(message, "老人", "儿童", "孕妇", "无障碍")) return "SPECIAL";
        return null;
    }

    private String detectPeopleType(String message) {
        if (containsAny(message, "老人")) return "ELDERLY";
        if (containsAny(message, "儿童", "孩子")) return "CHILDREN";
        if (containsAny(message, "孕妇")) return "PREGNANT";
        if (containsAny(message, "轮椅", "无障碍")) return "ACCESSIBILITY";
        return "GENERAL";
    }

    private String detectSortRule(String message) {
        if (containsAny(message, "最快", "少时间")) return "FASTEST";
        if (containsAny(message, "最便宜", "低价")) return "CHEAPEST";
        if (containsAny(message, "少换乘")) return "LESS_TRANSFER";
        return "SMART";
    }

    private List<String> detectReminderItems(String message) {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        if (containsAny(message, "出差", "开会", "客户", "公司", "总部", "园区", "商务", "会议", "拜访")) {
            items.addAll(List.of("身份证", "电脑", "充电器", "发票抬头", "工牌/门禁卡"));
        }
        if (containsAny(message, "文件", "材料", "合同", "PPT")) {
            items.add("会议文件");
        }
        return items.stream().distinct().toList();
    }

    private List<String> detectAmbiguousPlaces(String message, String endPlace) {
        // 明确地标和已确认目的地不视为模糊
        java.util.ArrayList<String> places = new java.util.ArrayList<>();
        for (String word : List.of("医院", "客户公司", "大学", "学校", "火车站", "总部")) {
            // 如果 endPlace 已确认且包含该词，跳过（用户已经在终点输入框确认过）
            if (endPlace != null && endPlace.contains(word)) continue;
            if (message.contains(word)) places.add(word);
        }
        // "阿里巴巴" 是明确地标，只有真正含糊时（如只说"园区"）才追问
        if (message.contains("园区") && !message.contains("阿里巴巴") && !message.contains("西溪") && !message.contains("滨江")) {
            places.add("园区");
        }
        return places.stream().distinct().toList();
    }

    private void applyBusinessTripHotelRule(String message, StructuredTravelDemand demand) {
        if (containsAny(message, "不需要酒店", "不用酒店", "不住酒店", "无需住宿")) {
            demand.setNeedHotel(false);
            demand.setHotelRequired(false);
            return;
        }
        if (containsAny(message, "需要酒店", "住宿", "住酒店", "住一晚", "房间", "预定酒店", "预订酒店", "订酒店", "帮我订", "酒店票")) {
            demand.setNeedHotel(true);
            demand.setHotelRequired(true);
            return;
        }
        if ("BUSINESS_TRIP".equals(demand.getSceneType()) && demand.getDays() != null && demand.getDays() >= 2) {
            demand.setNeedHotel(null);
            demand.setHotelRequired(null);
        }
    }

    private String normalizeCity(String value) {
        return value == null ? null : value.replace("的", "").replace("机票", "").replace("酒店", "").trim();
    }

    private void fillMissingFields(StructuredTravelDemand demand) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (demand.getStartPlace() == null || demand.getStartPlace().isBlank()) {
            missing.add("startPlace");
        }
        if (demand.getEndPlace() == null || demand.getEndPlace().isBlank()) {
            missing.add("endPlace");
        }
        demand.setMissingFields(missing);
        if (!missing.isEmpty()) {
            demand.setClarifyingQuestion("请补充" + String.join("、", missing) + "，我再继续生成完整出行方案。");
        } else {
            demand.setClarifyingQuestion(null);
        }
        if (demand.getNeedHotel() == null && demand.getHotelRequired() != null) {
            demand.setNeedHotel(Boolean.TRUE.equals(demand.getHotelRequired()));
        }
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
