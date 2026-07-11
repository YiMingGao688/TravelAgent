package com.itheima.travelagent2.modules.user.dto;

import lombok.Data;

@Data
public class AuthRequestDTO {
    private String account;
    private String phone;
    private String password;
}
