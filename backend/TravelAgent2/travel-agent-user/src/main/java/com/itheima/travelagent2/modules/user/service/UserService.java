package com.itheima.travelagent2.modules.user.service;

import com.itheima.travelagent2.modules.user.entity.User;

public interface UserService {
    User login(String phone, String password);
    User findByPhone(String phone);
    User findByUsername(String username);
    User findByAccount(String account);
    boolean hasPermission(Integer userId, String... permissions);
    void register(User user);
    java.util.List<User> listAll();
    void deleteById(Integer id);
}
