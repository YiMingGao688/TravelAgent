package com.itheima.travelagent2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String email;
    private String password;

    // 自动填充时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}