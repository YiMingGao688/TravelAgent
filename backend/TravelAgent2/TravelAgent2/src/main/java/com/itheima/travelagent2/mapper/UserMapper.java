package com.itheima.travelagent2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.travelagent2.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}