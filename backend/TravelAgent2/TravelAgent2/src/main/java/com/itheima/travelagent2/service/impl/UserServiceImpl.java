package com.itheima.travelagent2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.entity.User;
import com.itheima.travelagent2.mapper.UserMapper;
import com.itheima.travelagent2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String email, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        wrapper.eq(User::getPassword, password);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void register(User user) {
        userMapper.insert(user);
    }
}