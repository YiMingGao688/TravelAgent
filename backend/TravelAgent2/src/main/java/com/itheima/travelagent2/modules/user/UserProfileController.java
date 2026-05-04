package com.itheima.travelagent2.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.user.entity.UserProfile;
import com.itheima.travelagent2.modules.user.mapper.UserProfileMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileMapper profileMapper;

    public UserProfileController(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @GetMapping("/list")
    public Result<List<UserProfile>> list(@RequestParam(required = false) String userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            wrapper.eq(UserProfile::getUserId, userId);
        }
        wrapper.orderByDesc(UserProfile::getUpdatedAt);
        return Result.success(profileMapper.selectList(wrapper));
    }

    @GetMapping("/{id}")
    public Result<UserProfile> detail(@PathVariable Long id) {
        UserProfile profile = profileMapper.selectById(id);
        if (profile == null) {
            return Result.error(404, "Profile not found");
        }
        return Result.success(profile);
    }

    @PostMapping
    public Result<UserProfile> create(@RequestBody UserProfile profile) {
        if (profile.getUserId() == null || profile.getUserId().isBlank()) {
            return Result.error(400, "userId is required");
        }
        profileMapper.insert(profile);
        return Result.success("Created", profile);
    }

    @PutMapping("/{id}")
    public Result<UserProfile> update(@PathVariable Long id, @RequestBody UserProfile profile) {
        if (profileMapper.selectById(id) == null) {
            return Result.error(404, "Profile not found");
        }
        profile.setId(id);
        profileMapper.updateById(profile);
        return Result.success("Updated", profileMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        profileMapper.deleteById(id);
        return Result.success("Deleted");
    }
}
