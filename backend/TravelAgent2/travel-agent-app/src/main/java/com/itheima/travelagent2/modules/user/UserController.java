package com.itheima.travelagent2.modules.user;

import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user, HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        try {
            User loginUser = userService.login(user.getEmail(), user.getPassword());
            if (loginUser != null) {
                session.setAttribute("loginUser", loginUser);
                map.put("code", 200);
                map.put("msg", "login success");
            } else {
                map.put("code", 500);
                map.put("msg", "account or password error");
            }
        } catch (Exception e) {
            map.put("code", 500);
            map.put("msg", "system error");
        }
        return map;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        try {
            User exist = userService.findByEmail(user.getEmail());
            if (exist != null) {
                map.put("code", 500);
                map.put("msg", "email already registered");
                return map;
            }
            userService.register(user);
            map.put("code", 200);
            map.put("msg", "register success");
        } catch (Exception e) {
            map.put("code", 500);
            map.put("msg", "register failed");
        }
        return map;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        session.invalidate();
        map.put("code", 200);
        map.put("msg", "logout success");
        return map;
    }
}
