package com.itheima.travelagent2.modules.user.service;

import com.itheima.travelagent2.modules.user.entity.User;

public interface UserService {
    User login(String email, String password);
    User findByEmail(String email);
    void register(User user);
    java.util.List<User> listAll();
    void deleteById(Integer id);
}
