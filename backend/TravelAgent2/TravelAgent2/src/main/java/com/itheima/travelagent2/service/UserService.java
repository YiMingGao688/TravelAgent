package com.itheima.travelagent2.service;

import com.itheima.travelagent2.entity.User;

public interface UserService {
    User login(String email, String password);
    User findByEmail(String email);
    void register(User user);
}