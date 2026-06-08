package com.itheima.travelagent2.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserDTO {
    private String id;
    private String email;
    private String name;
    private String token;
}
