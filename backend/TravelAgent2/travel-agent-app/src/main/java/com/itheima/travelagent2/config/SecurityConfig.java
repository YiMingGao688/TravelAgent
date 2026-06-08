package com.itheima.travelagent2.config;

import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 当前项目先由前端弹窗触发登录/注册，业务接口暂不强制拦截，
     * 避免升级 Spring Security 后影响已有 AI、地图、订单等演示链路。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * Spring Security 登录时，根据账号查询用户信息。
     */
    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return username -> {
            User user = userService.findByAccount(username);

            if (user == null) {
                throw new UsernameNotFoundException("用户不存在");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities(List.of())
                    .build();
        };
    }

    /**
     * 密码加密器：
     * 新注册用户的密码必须使用 BCrypt 加密后存入数据库。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器：
     * 负责调用 UserDetailsService 查询用户，并使用 PasswordEncoder 校验密码。
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }
}