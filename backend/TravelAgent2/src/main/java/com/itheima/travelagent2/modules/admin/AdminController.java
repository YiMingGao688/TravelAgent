package com.itheima.travelagent2.modules.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.aftersale.AfterSaleOrderService;
import com.itheima.travelagent2.modules.aftersale.entity.AfterSaleOrder;
import com.itheima.travelagent2.modules.booking.entity.BookingOrder;
import com.itheima.travelagent2.modules.booking.entity.Hotel;
import com.itheima.travelagent2.modules.booking.entity.TrafficProduct;
import com.itheima.travelagent2.modules.booking.service.BookingOrderService;
import com.itheima.travelagent2.modules.booking.service.HotelService;
import com.itheima.travelagent2.modules.booking.service.TrafficProductService;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.chat.mapper.ChatRecordMapper;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.risk.RiskService;
import com.itheima.travelagent2.modules.risk.dto.RiskWarningDTO;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.route.mapper.RouteMapper;
import com.itheima.travelagent2.modules.route.service.RouteService;
import com.itheima.travelagent2.modules.servicereport.dto.TravelServiceReportSaveDTO;
import com.itheima.travelagent2.modules.servicereport.entity.TravelServiceReport;
import com.itheima.travelagent2.modules.servicereport.service.TravelServiceReportService;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;
import com.itheima.travelagent2.modules.smartservice.mapper.SmartServiceRecordMapper;
import com.itheima.travelagent2.modules.smartservice.service.SmartService;
import com.itheima.travelagent2.modules.timeline.TravelTimelineService;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.trip.mapper.TripPlanMapper;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.user.mapper.UserMapper;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import com.itheima.travelagent2.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TripPlanService tripPlanService;
    private final RouteService routeService;
    private final SmartService smartService;
    private final TravelServiceReportService reportService;
    private final ChatRecordMapper chatRecordMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final TripPlanMapper tripPlanMapper;
    private final RouteMapper routeMapper;
    private final SmartServiceRecordMapper smartServiceRecordMapper;
    private final BookingOrderService bookingOrderService;
    private final HotelService hotelService;
    private final TrafficProductService trafficProductService;
    private final TravelTimelineService timelineService;
    private final AfterSaleOrderService afterSaleOrderService;
    private final RiskService riskService;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.ai.zhipu.api-key:}")
    private String zhipuApiKey;

    @Value("${amap.key:${integration.amap-key:}}")
    private String amapKey;

    public AdminController(UserService userService,
                           TripPlanService tripPlanService,
                           RouteService routeService,
                           SmartService smartService,
                           TravelServiceReportService reportService,
                           ChatRecordMapper chatRecordMapper,
                           UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           TripPlanMapper tripPlanMapper,
                           RouteMapper routeMapper,
                           SmartServiceRecordMapper smartServiceRecordMapper,
                           BookingOrderService bookingOrderService,
                           HotelService hotelService,
                           TrafficProductService trafficProductService,
                           TravelTimelineService timelineService,
                           AfterSaleOrderService afterSaleOrderService,
                           RiskService riskService,
                           PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tripPlanService = tripPlanService;
        this.routeService = routeService;
        this.smartService = smartService;
        this.reportService = reportService;
        this.chatRecordMapper = chatRecordMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.tripPlanMapper = tripPlanMapper;
        this.routeMapper = routeMapper;
        this.smartServiceRecordMapper = smartServiceRecordMapper;
        this.bookingOrderService = bookingOrderService;
        this.hotelService = hotelService;
        this.trafficProductService = trafficProductService;
        this.timelineService = timelineService;
        this.afterSaleOrderService = afterSaleOrderService;
        this.riskService = riskService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        User admin = userService.findByAccount(username);
        boolean passwordMatched = admin != null
                && admin.getPassword() != null
                && (passwordEncoder.matches(password, admin.getPassword()) || password.equals(admin.getPassword()));
        if (admin != null
                && ("ADMIN".equalsIgnoreCase(admin.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(admin.getRole()))
                && !"DISABLED".equalsIgnoreCase(admin.getStatus())
                && passwordMatched) {
            if (password.equals(admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(password));
            }
            admin.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(admin);
            Map<String, Object> data = new HashMap<>();
            data.put("token", "simple-admin-token");
            data.put("username", admin.getUsername());
            data.put("role", admin.getRole());
            data.put("userId", admin.getId());
            return Result.success("login success", data);
        }
        return Result.error(401, "admin username or password is incorrect");
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        List<User> users = userService.listAll();
        List<TripPlan> tripPlans = tripPlanService.listAll();
        List<Route> routes = routeService.listAll();
        List<SmartServiceRecord> services = smartService.listAll();
        List<TravelServiceReport> reports = reportService.listAll(null);
        List<BookingOrder> orders = bookingOrderService.listAll();
        List<TravelTimelineItem> timelines = timelineService.listAll();
        List<AfterSaleOrder> afterSales = afterSaleOrderService.listAll();
        Long chatRecordCount = chatRecordMapper.selectCount(null);
        Long profileCount = userProfileMapper.selectCount(null);

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", users.size());
        data.put("tripPlanCount", tripPlans.size());
        data.put("chatRecordCount", chatRecordCount);
        data.put("routeCount", routes.size());
        data.put("smartServiceCount", services.size());
        data.put("serviceReportCount", reports.size());
        data.put("bookingOrderCount", orders.size());
        data.put("timelineCount", timelines.size());
        data.put("afterSaleCount", afterSales.size());
        data.put("profileCount", profileCount);
        data.put("todayNewCount", countToday(users, tripPlans, routes, services, reports, orders));
        data.put("last7Days", last7Days());
        data.put("userTrend", trendUsers(users));
        data.put("tripPlanTrend", trendOrders(orders));
        data.put("reportTrend", trendReports(reports));
        data.put("preferenceDistribution", distribution(orders, BookingOrder::getOrderType, "未知订单"));
        data.put("serviceTypeDistribution", distribution(orders, BookingOrder::getStatus, "未知状态"));
        // 城市热度必须来自真实下单行为：用户下单后 booking_order.start_place / end_place 会实时进入排名。
        data.put("popularStartPlaces", topPlaces(orders, BookingOrder::getStartPlace));
        data.put("popularEndPlaces", topPlaces(orders, BookingOrder::getEndPlace));
        data.put("systemStatus", buildSystemStatus());
        return Result.success(data);
    }

    private Map<String, Object> buildSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("database", "正常");
        status.put("ai", isConfigured(zhipuApiKey) ? "正常" : "未配置");
        status.put("amap", isConfigured(amapKey) ? "正常" : "未配置");
        status.put("apiCallsToday", "待接入");
        return status;
    }

    private boolean isConfigured(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("placeholder")
                && !value.contains("please-fill");
    }

    @GetMapping("/users")
    public Result<List<User>> users() {
        return Result.success(userService.listAll());
    }

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Result.error(400, "email is required");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword("123456");
        }
        userService.register(user);
        return Result.success("created", user);
    }

    @PutMapping("/users/{id}")
    public Result<String> updateUser(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        if (user.getPassword() != null && user.getPassword().isBlank()) {
            user.setPassword(null);
        } else if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(user);
        return Result.success("updated");
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Integer id) {
        User target = userMapper.selectById(id);
        if (target != null && "SUPER_ADMIN".equalsIgnoreCase(target.getRole())) {
            Long superAdminCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "SUPER_ADMIN"));
            if (superAdminCount <= 1) {
                return Result.error(400, "至少需要保留一个超级管理员账号");
            }
        }
        userService.deleteById(id);
        return Result.success("deleted");
    }

    @GetMapping("/trip-plans")
    public Result<List<TripPlan>> tripPlans() {
        return Result.success(tripPlanService.listAll());
    }

    @PostMapping("/trip-plans")
    public Result<TripPlan> createTripPlan(@RequestBody TripPlan plan) {
        tripPlanMapper.insert(plan);
        return Result.success("created", plan);
    }

    @PutMapping("/trip-plans/{id}")
    public Result<String> updateTripPlan(@PathVariable Long id, @RequestBody TripPlan plan) {
        plan.setId(id);
        tripPlanMapper.updateById(plan);
        return Result.success("updated");
    }

    @DeleteMapping("/trip-plans/{id}")
    public Result<String> deleteTripPlan(@PathVariable Long id) {
        System.out.println("管理端收到历史方案删除请求，id = " + id);
        if (!tripPlanService.deleteById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的历史方案");
        }
        return Result.success("deleted");
    }

    @GetMapping("/chat-records")
    public Result<List<ChatRecord>> chatRecords() {
        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatRecord::getCreateTime);
        return Result.success(chatRecordMapper.selectList(wrapper));
    }

    @PostMapping("/chat-records")
    public Result<ChatRecord> createChatRecord(@RequestBody ChatRecord record) {
        chatRecordMapper.insert(record);
        return Result.success("created", record);
    }

    @PutMapping("/chat-records/{id}")
    public Result<String> updateChatRecord(@PathVariable Long id, @RequestBody ChatRecord record) {
        record.setId(id);
        chatRecordMapper.updateById(record);
        return Result.success("updated");
    }

    @DeleteMapping("/chat-records/{id}")
    public Result<String> deleteChatRecord(@PathVariable Long id) {
        System.out.println("管理端收到聊天记录删除请求，id = " + id);
        if (chatRecordMapper.deleteById(id) <= 0) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的聊天记录");
        }
        return Result.success("deleted");
    }

    @GetMapping("/profiles")
    public Result<List<UserProfile>> profiles() {
        return Result.success(userProfileMapper.selectList(null));
    }

    @PostMapping("/profiles")
    public Result<UserProfile> createProfile(@RequestBody UserProfile profile) {
        userProfileMapper.insert(profile);
        return Result.success("created", profile);
    }

    @PutMapping("/profiles/{id}")
    public Result<String> updateProfile(@PathVariable Long id, @RequestBody UserProfile profile) {
        profile.setId(id);
        userProfileMapper.updateById(profile);
        return Result.success("updated");
    }

    @DeleteMapping("/profiles/{id}")
    public Result<String> deleteProfile(@PathVariable Long id) {
        System.out.println("管理端收到用户偏好删除请求，id = " + id);
        if (userProfileMapper.deleteById(id) <= 0) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的用户偏好");
        }
        return Result.success("deleted");
    }

    @GetMapping("/routes")
    public Result<List<Route>> routes() {
        return Result.success(routeService.listAll());
    }

    @GetMapping("/orders")
    public Result<List<BookingOrder>> orders() {
        return Result.success(bookingOrderService.listAll());
    }

    @GetMapping("/hotels")
    public Result<List<Hotel>> hotels() {
        return Result.success(hotelService.listAll());
    }

    @GetMapping("/traffic")
    public Result<List<TrafficProduct>> traffic() {
        return Result.success(trafficProductService.listAll());
    }

    @GetMapping("/risks")
    public Result<List<RiskWarningDTO>> risks() {
        StructuredTravelDemand demand = new StructuredTravelDemand();
        demand.setPeopleType("ELDERLY");
        demand.setLuggageInfo("有行李");
        demand.setTransportPreference("TRAIN");
        return Result.success(riskService.generateWarnings(demand, null));
    }

    @GetMapping("/timelines")
    public Result<List<TravelTimelineItem>> timelines() {
        return Result.success(timelineService.listAll());
    }

    @GetMapping("/after-sales")
    public Result<List<AfterSaleOrder>> afterSales() {
        return Result.success(afterSaleOrderService.listAll());
    }

    @PutMapping("/after-sales/{id}")
    public Result<AfterSaleOrder> updateAfterSale(@PathVariable Long id, @RequestBody AfterSaleOrder order) {
        AfterSaleOrder updated = afterSaleOrderService.updateStatus(id, order);
        return updated == null ? Result.error(404, "售后工单不存在") : Result.success(updated);
    }

    @DeleteMapping("/orders/{id}")
    public Result<String> deleteOrder(@PathVariable Long id) {
        if (!bookingOrderService.removeById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的模拟订单");
        }
        return Result.success("deleted");
    }

    @DeleteMapping("/hotels/{id}")
    public Result<String> deleteHotel(@PathVariable Long id) {
        if (!hotelService.removeById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的酒店数据");
        }
        return Result.success("deleted");
    }

    @DeleteMapping("/traffic/{id}")
    public Result<String> deleteTraffic(@PathVariable Long id) {
        if (!trafficProductService.removeById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的交通产品");
        }
        return Result.success("deleted");
    }

    @PostMapping("/routes")
    public Result<Route> createRoute(@RequestBody Route route) {
        routeMapper.insert(route);
        return Result.success("created", route);
    }

    @PutMapping("/routes/{id}")
    public Result<String> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        route.setId(id);
        routeMapper.updateById(route);
        return Result.success("updated");
    }

    @DeleteMapping("/routes/{id}")
    public Result<String> deleteRoute(@PathVariable Long id) {
        System.out.println("管理端收到路线记录删除请求，id = " + id);
        if (!routeService.deleteById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的路线记录");
        }
        return Result.success("deleted");
    }

    @GetMapping("/smart-services")
    public Result<List<SmartServiceRecord>> smartServices() {
        return Result.success(smartService.listAll());
    }

    @PostMapping("/smart-services")
    public Result<SmartServiceRecord> createSmartService(@RequestBody SmartServiceRecord record) {
        smartServiceRecordMapper.insert(record);
        return Result.success("created", record);
    }

    @PutMapping("/smart-services/{id}")
    public Result<String> updateSmartService(@PathVariable Long id, @RequestBody SmartServiceRecord record) {
        record.setId(id);
        smartServiceRecordMapper.updateById(record);
        return Result.success("updated");
    }

    @DeleteMapping("/smart-services/{id}")
    public Result<String> deleteSmartService(@PathVariable Long id) {
        System.out.println("管理端收到服务记录删除请求，id = " + id);
        if (!smartService.deleteById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的服务记录");
        }
        return Result.success("deleted");
    }

    @GetMapping("/service-reports")
    public Result<List<TravelServiceReport>> serviceReports() {
        return Result.success(reportService.listAll(null));
    }

    @PostMapping("/service-reports")
    public Result<TravelServiceReport> createServiceReport(@RequestBody TravelServiceReportSaveDTO dto) {
        return Result.success("created", reportService.save(dto));
    }

    @PutMapping("/service-reports/{id}")
    public Result<TravelServiceReport> updateServiceReport(@PathVariable Long id,
                                                           @RequestBody TravelServiceReportSaveDTO dto) {
        return Result.success("updated", reportService.update(id, dto));
    }

    @DeleteMapping("/service-reports/{id}")
    public Result<String> deleteServiceReport(@PathVariable Long id) {
        System.out.println("管理端收到服务报告删除请求，id = " + id);
        if (!reportService.deleteById(id)) {
            return Result.error("删除失败，数据库中不存在 id = " + id + " 的服务报告");
        }
        return Result.success("deleted");
    }

    private long countToday(List<User> users,
                            List<TripPlan> tripPlans,
                            List<Route> routes,
                            List<SmartServiceRecord> services,
                            List<TravelServiceReport> reports,
                            List<BookingOrder> orders) {
        return users.stream().filter(item -> isToday(item.getCreateTime())).count()
                + tripPlans.stream().filter(item -> isToday(item.getCreateTime())).count()
                + routes.stream().filter(item -> isToday(item.getCreateTime())).count()
                + services.stream().filter(item -> isToday(item.getCreateTime())).count()
                + reports.stream().filter(item -> isToday(item.getCreateTime())).count()
                + orders.stream().filter(item -> isToday(item.getCreateTime())).count();
    }

    private boolean isToday(LocalDateTime value) {
        return value != null && value.toLocalDate().equals(LocalDate.now());
    }

    private List<String> last7Days() {
        List<String> labels = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            labels.add(start.plusDays(i).toString().substring(5));
        }
        return labels;
    }

    private List<Long> trendUsers(List<User> users) {
        return trend(users.stream().map(User::getCreateTime).collect(Collectors.toList()));
    }

    private List<Long> trendTripPlans(List<TripPlan> plans) {
        return trend(plans.stream().map(TripPlan::getCreateTime).collect(Collectors.toList()));
    }

    private List<Long> trendOrders(List<BookingOrder> orders) {
        return trend(orders.stream().map(BookingOrder::getCreateTime).collect(Collectors.toList()));
    }

    private List<Long> trendReports(List<TravelServiceReport> reports) {
        return trend(reports.stream().map(TravelServiceReport::getCreateTime).collect(Collectors.toList()));
    }

    private List<Long> trend(List<LocalDateTime> times) {
        List<Long> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            result.add(times.stream().filter(time -> time != null && day.equals(time.toLocalDate())).count());
        }
        return result;
    }

    private <T> Map<String, Long> distribution(List<T> rows, Function<T, String> getter, String emptyName) {
        return rows.stream()
                .map(getter)
                .map(value -> value == null || value.isBlank() ? emptyName : value)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
    }

    private <T> Map<String, Long> topPlaces(List<T> rows, Function<T, String> getter) {
        return rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
