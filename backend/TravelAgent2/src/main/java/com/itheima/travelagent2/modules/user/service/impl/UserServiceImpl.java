package com.itheima.travelagent2.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.mapper.UserMapper;
import com.itheima.travelagent2.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    public User findByAccount(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, account)
                .or().eq(User::getUsername, account)
                .or().eq(User::getEmail, account);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void register(User user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ENABLED");
        }
        String account = firstText(user.getPhone(), user.getUsername(), user.getEmail());
        if ((user.getUsername() == null || user.getUsername().isBlank()) && account != null) {
            user.setUsername(account);
        }
        if ((user.getNickname() == null || user.getNickname().isBlank()) && account != null) {
            user.setNickname(account);
        }
        if ((user.getEmail() == null || user.getEmail().isBlank()) && account != null) {
            user.setEmail(account + "@phone.local");
        }
        if (user.getPassword() != null && !isEncoded(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
    }

    @Override
    public List<User> listAll() {
        return userMapper.selectList(null);
    }

    @Override
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

    private boolean isEncoded(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
