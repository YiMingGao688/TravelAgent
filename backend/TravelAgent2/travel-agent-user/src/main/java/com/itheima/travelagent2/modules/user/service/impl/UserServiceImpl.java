package com.itheima.travelagent2.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.entity.UserPermission;
import com.itheima.travelagent2.modules.user.mapper.UserMapper;
import com.itheima.travelagent2.modules.user.mapper.UserPermissionMapper;
import com.itheima.travelagent2.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(String phone, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        wrapper.eq(User::getPassword, password);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User findByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return attachPermission(userMapper.selectOne(wrapper));
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return attachPermission(userMapper.selectOne(wrapper));
    }

    @Override
    public User findByAccount(String account) {
        return findByPhone(account);
    }

    @Override
    public boolean hasPermission(Integer userId, String... permissions) {
        if (userId == null || permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> allowed = Arrays.stream(permissions)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .toList();
        return userPermissionMapper.selectCount(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getStatus, "ENABLED")
                .in(UserPermission::getPermissionCode, allowed)) > 0;
    }

    @Override
    public void register(User user) {
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ENABLED");
        }
        String account = firstText(user.getPhone(), user.getUsername());
        if ((user.getUsername() == null || user.getUsername().isBlank()) && account != null) {
            user.setUsername(account);
        }
        if ((user.getNickname() == null || user.getNickname().isBlank()) && account != null) {
            user.setNickname(account);
        }
        if (user.getPassword() != null && !isEncoded(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
        grantPermissionIfMissing(user.getId(), firstText(user.getPermission(), "USER"));
    }

    @Override
    public List<User> listAll() {
        return userMapper.selectList(null).stream()
                .map(this::attachPermission)
                .toList();
    }

    @Override
    public void deleteById(Integer id) {
        userPermissionMapper.delete(new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, id));
        userMapper.deleteById(id);
    }

    private User attachPermission(User user) {
        if (user == null || user.getId() == null) {
            return user;
        }
        UserPermission permission = userPermissionMapper.selectOne(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, user.getId())
                .eq(UserPermission::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (permission != null) {
            user.setPermission(permission.getPermissionCode());
        }
        return user;
    }

    private void grantPermissionIfMissing(Integer userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return;
        }
        String code = permissionCode.trim().toUpperCase();
        Long count = userPermissionMapper.selectCount(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionCode, code));
        if (count > 0) {
            return;
        }
        UserPermission permission = new UserPermission();
        permission.setUserId(userId);
        permission.setPermissionCode(code);
        permission.setPermissionName(code);
        permission.setStatus("ENABLED");
        userPermissionMapper.insert(permission);
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
