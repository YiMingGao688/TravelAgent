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
/**
 * 用户出行画像接口。
 * 画像用于保存常驻城市、偏好、备注等默认信息，AI 咨询会把它作为当前会话上下文的兜底来源。
 */
public class UserProfileController {

    private final UserProfileMapper profileMapper;

    public UserProfileController(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @GetMapping("/list")
    /**
     * 查询用户画像。
     * userId 为空时可给管理端查看全部画像，客户端一般按当前登录用户查询。
     */
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
    /**
     * 更新用户画像。
     * 修改后前端会同步当前 TripContext，让后续咨询立刻读取最新偏好。
     */
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
