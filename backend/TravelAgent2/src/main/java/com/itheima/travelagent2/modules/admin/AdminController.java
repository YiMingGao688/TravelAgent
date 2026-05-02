package com.itheima.travelagent2.modules.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.chat.entity.ChatRecord;
import com.itheima.travelagent2.modules.route.entity.Route;
import com.itheima.travelagent2.modules.smartservice.entity.SmartServiceRecord;
import com.itheima.travelagent2.modules.trip.entity.TripPlan;
import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.chat.mapper.ChatRecordMapper;
import com.itheima.travelagent2.modules.route.mapper.RouteMapper;
import com.itheima.travelagent2.modules.smartservice.mapper.SmartServiceRecordMapper;
import com.itheima.travelagent2.modules.trip.mapper.TripPlanMapper;
import com.itheima.travelagent2.modules.user.mapper.UserMapper;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import com.itheima.travelagent2.modules.route.service.RouteService;
import com.itheima.travelagent2.modules.smartservice.service.SmartService;
import com.itheima.travelagent2.modules.trip.service.TripPlanService;
import com.itheima.travelagent2.modules.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TripPlanService tripPlanService;
    private final RouteService routeService;
    private final SmartService smartService;
    private final ChatRecordMapper chatRecordMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final TripPlanMapper tripPlanMapper;
    private final RouteMapper routeMapper;
    private final SmartServiceRecordMapper smartServiceRecordMapper;

    public AdminController(UserService userService,
                           TripPlanService tripPlanService,
                           RouteService routeService,
                           SmartService smartService,
                           ChatRecordMapper chatRecordMapper,
                           UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           TripPlanMapper tripPlanMapper,
                           RouteMapper routeMapper,
                           SmartServiceRecordMapper smartServiceRecordMapper) {
        this.userService = userService;
        this.tripPlanService = tripPlanService;
        this.routeService = routeService;
        this.smartService = smartService;
        this.chatRecordMapper = chatRecordMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.tripPlanMapper = tripPlanMapper;
        this.routeMapper = routeMapper;
        this.smartServiceRecordMapper = smartServiceRecordMapper;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if ("admin".equals(username) && "admin123".equals(password)) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", "simple-admin-token");
            data.put("username", username);
            return Result.success("登录成功", data);
        }
        return Result.error(401, "管理员账号或密码错误");
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userService.listAll().size());
        data.put("tripPlanCount", tripPlanService.listAll().size());
        data.put("chatRecordCount", chatRecordMapper.selectCount(null));
        data.put("routeCount", routeService.listAll().size());
        data.put("smartServiceCount", smartService.listAll().size());
        data.put("profileCount", userProfileMapper.selectCount(null));
        return Result.success(data);
    }

    @GetMapping("/users")
    public Result<List<User>> users() {
        return Result.success(userService.listAll());
    }

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Result.error(400, "邮箱不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword("123456");
        }
        userMapper.insert(user);
        return Result.success("新增成功", user);
    }

    @PutMapping("/users/{id}")
    public Result<String> updateUser(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        if (user.getPassword() != null && user.getPassword().isBlank()) {
            user.setPassword(null);
        }
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Integer id) {
        userService.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/trip-plans")
    public Result<List<TripPlan>> tripPlans() {
        return Result.success(tripPlanService.listAll());
    }

    @PostMapping("/trip-plans")
    public Result<TripPlan> createTripPlan(@RequestBody TripPlan plan) {
        tripPlanMapper.insert(plan);
        return Result.success("新增成功", plan);
    }

    @PutMapping("/trip-plans/{id}")
    public Result<String> updateTripPlan(@PathVariable Long id, @RequestBody TripPlan plan) {
        plan.setId(id);
        tripPlanMapper.updateById(plan);
        return Result.success("更新成功");
    }

    @DeleteMapping("/trip-plans/{id}")
    public Result<String> deleteTripPlan(@PathVariable Long id) {
        tripPlanService.deleteById(id);
        return Result.success("删除成功");
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
        return Result.success("Created", record);
    }

    @PutMapping("/chat-records/{id}")
    public Result<String> updateChatRecord(@PathVariable Long id, @RequestBody ChatRecord record) {
        record.setId(id);
        chatRecordMapper.updateById(record);
        return Result.success("更新成功");
    }

    @DeleteMapping("/chat-records/{id}")
    public Result<String> deleteChatRecord(@PathVariable Long id) {
        chatRecordMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/profiles")
    public Result<List<UserProfile>> profiles() {
        return Result.success(userProfileMapper.selectList(null));
    }

    @PostMapping("/profiles")
    public Result<UserProfile> createProfile(@RequestBody UserProfile profile) {
        userProfileMapper.insert(profile);
        return Result.success("新增成功", profile);
    }

    @PutMapping("/profiles/{id}")
    public Result<String> updateProfile(@PathVariable Long id, @RequestBody UserProfile profile) {
        profile.setId(id);
        userProfileMapper.updateById(profile);
        return Result.success("更新成功");
    }

    @DeleteMapping("/profiles/{id}")
    public Result<String> deleteProfile(@PathVariable Long id) {
        userProfileMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/routes")
    public Result<List<Route>> routes() {
        return Result.success(routeService.listAll());
    }

    @PostMapping("/routes")
    public Result<Route> createRoute(@RequestBody Route route) {
        routeMapper.insert(route);
        return Result.success("新增成功", route);
    }

    @PutMapping("/routes/{id}")
    public Result<String> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        route.setId(id);
        routeMapper.updateById(route);
        return Result.success("更新成功");
    }

    @DeleteMapping("/routes/{id}")
    public Result<String> deleteRoute(@PathVariable Long id) {
        routeService.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/smart-services")
    public Result<List<SmartServiceRecord>> smartServices() {
        return Result.success(smartService.listAll());
    }

    @PostMapping("/smart-services")
    public Result<SmartServiceRecord> createSmartService(@RequestBody SmartServiceRecord record) {
        smartServiceRecordMapper.insert(record);
        return Result.success("新增成功", record);
    }

    @PutMapping("/smart-services/{id}")
    public Result<String> updateSmartService(@PathVariable Long id, @RequestBody SmartServiceRecord record) {
        record.setId(id);
        smartServiceRecordMapper.updateById(record);
        return Result.success("更新成功");
    }

    @DeleteMapping("/smart-services/{id}")
    public Result<String> deleteSmartService(@PathVariable Long id) {
        smartService.deleteById(id);
        return Result.success("删除成功");
    }
}
