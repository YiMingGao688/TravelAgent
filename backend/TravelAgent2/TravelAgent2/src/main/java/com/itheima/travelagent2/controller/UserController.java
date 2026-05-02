package com.itheima.travelagent2.controller;

import com.itheima.travelagent2.entity.User;
import com.itheima.travelagent2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 登录
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        try {
            User loginUser = userService.login(user.getEmail(), user.getPassword());
            if (loginUser != null) {
                map.put("code", 200);
                map.put("msg", "登录成功");
            } else {
                map.put("code", 500);
                map.put("msg", "账号或密码错误");
            }
        } catch (Exception e) {
            map.put("code", 500);
            map.put("msg", "系统异常");
        }
        return map;
    }

    // 注册 → 就是这里！你之前没有！
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        try {
            // 检查邮箱是否已存在
            User exist = userService.findByEmail(user.getEmail());
            if (exist != null) {
                map.put("code", 500);
                map.put("msg", "邮箱已被注册");
                return map;
            }

            // 执行注册
            userService.register(user);
            map.put("code", 200);
            map.put("msg", "注册成功");
        } catch (Exception e) {
            map.put("code", 500);
            map.put("msg", "注册失败");
        }
        return map;
    }
}