package com.itheima.travelagent2.modules.assistant;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.ai.ZhipuAiClient;
import com.itheima.travelagent2.modules.booking.dto.OrderCreateDTO;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import com.itheima.travelagent2.modules.booking.service.HotelService;
import com.itheima.travelagent2.modules.booking.service.TrafficProductService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantAgentController {

    private final TrafficProductService trafficProductService;
    private final HotelService hotelService;
    private final ZhipuAiClient zhipuAiClient;

    public AssistantAgentController(TrafficProductService trafficProductService,
                                    HotelService hotelService,
                                    ZhipuAiClient zhipuAiClient) {
        this.trafficProductService = trafficProductService;
        this.hotelService = hotelService;
        this.zhipuAiClient = zhipuAiClient;
    }

    @PostMapping("/ask")
    public Result<AssistantResponse> ask(@RequestBody AssistantRequest request) {
        String message = trimToEmpty(request.message());
        if (message.isBlank()) {
            return Result.error(400, "请输入问题");
        }

        List<String> steps = new ArrayList<>();
        steps.add("识别用户输入：" + message);
        steps.add("出行场景：" + defaultText(request.scene(), "旅游场景"));

        String type = detectOrderType(message);
        if (type != null) {
            steps.add("识别到预订意图：" + readableType(type));
            if ("HOTEL".equals(type)) {
                return Result.success(buildHotelProposal(request, message, steps));
            }
            return Result.success(buildTrafficProposal(request, message, type, steps));
        }

        steps.add("未识别到下单意图，转入大模型问答");
        String reply;
        try {
            reply = zhipuAiClient.chat(
                    "你是七维智行的出行助手。请用简体中文，结合用户选择的出行场景回答，回答要具体可执行。",
                    "场景：" + defaultText(request.scene(), "旅游场景") + "\n用户问题：" + message
            );
        } catch (Exception e) {
            reply = "我可以回答路线、住宿、交通和订单相关问题。当前 AI 接口调用失败：" + e.getMessage();
        }
        AssistantResponse response = new AssistantResponse();
        response.setAction("CHAT");
        response.setReply(reply);
        response.setThoughtSteps(steps);
        return Result.success(response);
    }

    private AssistantResponse buildTrafficProposal(AssistantRequest request, String message, String type, List<String> steps) {
        String from = firstNonBlank(request.from(), extractStart(message), defaultStart(type));
        String to = firstNonBlank(request.destination(), extractDestination(message), "杭州");
        steps.add("抽取出发地：" + from + "，目的地：" + to);
        steps.add("查询数据库 traffic_product 表，按类型、出发地、目的地匹配可售产品");

        List<TrafficProduct> products = trafficProductService.search(type, from, to, null);
        if (products.isEmpty()) {
            products = trafficProductService.search(type, null, to, null);
        }
        if (products.isEmpty()) {
            products = trafficProductService.search(type, null, null, null);
        }

        AssistantResponse response = new AssistantResponse();
        response.setAction("BOOKING_PROPOSAL");
        response.setThoughtSteps(steps);
        response.setCandidates(products.stream().limit(3).map(this::trafficCandidate).toList());

        if (products.isEmpty()) {
            response.setReply("数据库里暂时没有匹配的" + readableType(type) + "产品，不能生成订单。");
            return response;
        }

        TrafficProduct selected = products.get(0);
        steps.add("选择价格和时间综合最靠前的产品：" + selected.getCarrierName());
        OrderCreateDTO payload = new OrderCreateDTO();
        payload.setUserId(defaultText(request.userId(), "test_user"));
        payload.setOrderType(type);
        payload.setProductId(selected.getId());
        payload.setProductName(selected.getCarrierName() + " · " + selected.getSeatType());
        payload.setStartPlace(selected.getStartPlace());
        payload.setEndPlace(selected.getEndPlace());
        payload.setStartTime(selected.getDepartTime());
        payload.setEndTime(selected.getArriveTime());
        payload.setTotalAmount(selected.getPrice());
        payload.setRemark("由右下角智能体根据用户意图生成，待用户一键确认");
        response.setOrderPayload(payload);
        response.setReply("我已找到可预订的" + readableType(type) + "：\n"
                + selected.getCarrierName() + "，" + selected.getStartPlace() + " 到 " + selected.getEndPlace()
                + "，" + selected.getSeatType() + "，价格 " + selected.getPrice() + " 元。\n点击“一键生成订单”后会写入数据库，再点“确认订单”完成模拟支付。");
        return response;
    }

    private AssistantResponse buildHotelProposal(AssistantRequest request, String message, List<String> steps) {
        String destination = firstNonBlank(request.destination(), extractDestination(message), "杭州");
        steps.add("抽取住宿目的地：" + destination);
        steps.add("查询数据库 hotel 表，匹配城市/酒店/标签");

        List<Hotel> hotels = hotelService.search(destination, destination, null, null);
        if (hotels.isEmpty()) {
            hotels = hotelService.search(null, destination, null, null);
        }
        if (hotels.isEmpty()) {
            hotels = hotelService.search(null, null, null, null);
        }

        AssistantResponse response = new AssistantResponse();
        response.setAction("BOOKING_PROPOSAL");
        response.setThoughtSteps(steps);
        response.setCandidates(hotels.stream().limit(3).map(this::hotelCandidate).toList());
        if (hotels.isEmpty()) {
            response.setReply("数据库里暂时没有可预订酒店，不能生成订单。");
            return response;
        }

        Hotel selected = hotels.get(0);
        steps.add("选择评分和价格综合最靠前的酒店：" + selected.getName());
        OrderCreateDTO payload = new OrderCreateDTO();
        payload.setUserId(defaultText(request.userId(), "test_user"));
        payload.setOrderType("HOTEL");
        payload.setProductId(selected.getId());
        payload.setProductName(selected.getName());
        payload.setStartPlace(defaultText(request.from(), "当前位置"));
        payload.setEndPlace(selected.getCity());
        payload.setStartTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0));
        payload.setEndTime(LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0));
        payload.setTotalAmount(selected.getMinPrice() == null ? BigDecimal.ZERO : selected.getMinPrice());
        payload.setRemark("由右下角智能体根据用户意图生成，待用户一键确认");
        response.setOrderPayload(payload);
        response.setReply("我已找到可预订酒店：" + selected.getName() + "，城市 " + selected.getCity()
                + "，参考价 " + selected.getMinPrice() + " 元。点击“一键生成订单”即可写入数据库。");
        return response;
    }

    private Map<String, Object> trafficCandidate(TrafficProduct product) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", product.getId());
        item.put("type", product.getType());
        item.put("title", product.getCarrierName());
        item.put("route", product.getStartPlace() + " -> " + product.getEndPlace());
        item.put("time", product.getDepartTime());
        item.put("price", product.getPrice());
        item.put("remainCount", product.getRemainCount());
        return item;
    }

    private Map<String, Object> hotelCandidate(Hotel hotel) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", hotel.getId());
        item.put("type", "HOTEL");
        item.put("title", hotel.getName());
        item.put("route", hotel.getCity());
        item.put("price", hotel.getMinPrice());
        item.put("score", hotel.getScore());
        item.put("tags", hotel.getTags());
        return item;
    }

    private String detectOrderType(String message) {
        if (!containsAny(message, "买", "订", "预订", "下单", "购买", "帮我定", "给我买")) {
            return null;
        }
        if (containsAny(message, "机票", "飞机", "航班")) return "FLIGHT";
        if (containsAny(message, "高铁", "火车", "动车", "车票")) return "TRAIN";
        if (containsAny(message, "酒店", "住宿", "房间")) return "HOTEL";
        if (containsAny(message, "公交", "大巴")) return "BUS";
        if (containsAny(message, "地铁")) return "METRO";
        if (containsAny(message, "车", "打车", "网约车", "自驾")) return "TAXI";
        return "FLIGHT";
    }

    private String extractDestination(String message) {
        for (String city : List.of("杭州", "南京", "上海", "苏州", "北京", "成都")) {
            if (message.contains(city)) return city;
        }
        return "";
    }

    private String extractStart(String message) {
        if (message.contains("南京")) return "南京";
        if (message.contains("上海")) return "上海";
        if (message.contains("北京")) return "北京";
        if (message.contains("成都")) return "成都";
        return "";
    }

    private String defaultStart(String type) {
        if ("FLIGHT".equals(type)) return "南京";
        if ("TRAIN".equals(type)) return "南京";
        return "";
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String readableType(String type) {
        return switch (type) {
            case "FLIGHT" -> "机票";
            case "TRAIN" -> "高铁票";
            case "HOTEL" -> "酒店";
            case "BUS" -> "巴士票";
            case "METRO" -> "地铁";
            case "TAXI" -> "用车";
            default -> type;
        };
    }

    public record AssistantRequest(String message, String scene, String userId, String from, String destination) {
    }

    public static class AssistantResponse {
        private String action;
        private String reply;
        private List<String> thoughtSteps;
        private List<Map<String, Object>> candidates;
        private OrderCreateDTO orderPayload;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }

        public List<String> getThoughtSteps() {
            return thoughtSteps;
        }

        public void setThoughtSteps(List<String> thoughtSteps) {
            this.thoughtSteps = thoughtSteps;
        }

        public List<Map<String, Object>> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Map<String, Object>> candidates) {
            this.candidates = candidates;
        }

        public OrderCreateDTO getOrderPayload() {
            return orderPayload;
        }

        public void setOrderPayload(OrderCreateDTO orderPayload) {
            this.orderPayload = orderPayload;
        }
    }
}
