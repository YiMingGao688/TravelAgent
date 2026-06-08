package com.itheima.travelagent2.modules.user;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.user.dto.AuthRequestDTO;
import com.itheima.travelagent2.modules.user.dto.AuthUserDTO;
import com.itheima.travelagent2.modules.user.entity.User;
import com.itheima.travelagent2.modules.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<AuthUserDTO> login(@RequestBody AuthRequestDTO request, HttpSession session) {
        String account = normalizeAccount(request);
        if (account == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return Result.error(400, "请输入手机号和密码");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account, request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        User user = userService.findByAccount(account);
        AuthUserDTO dto = toAuthUser(user, session);
        session.setAttribute("loginUser", dto);
        return Result.success("登录成功", dto);
    }

    @PostMapping("/register")
    public Result<AuthUserDTO> register(@RequestBody AuthRequestDTO request, HttpSession session) {
        String account = normalizeAccount(request);
        if (account == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return Result.error(400, "请输入手机号和密码");
        }
        if (userService.findByAccount(account) != null) {
            return Result.error(409, "该账号已注册");
        }

        User user = new User();
        user.setPhone(account);
        user.setUsername(account);
        user.setNickname(account);
        user.setEmail(account + "@phone.local");
        user.setPassword(request.getPassword());
        userService.register(user);
        return login(request, session);
    }

    @GetMapping("/me")
    public Result<AuthUserDTO> me(HttpSession session) {
        Object value = session.getAttribute("loginUser");
        if (value instanceof AuthUserDTO dto) {
            return Result.success(dto);
        }
        return Result.error(401, "未登录");
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return Result.success();
    }

    private String normalizeAccount(AuthRequestDTO request) {
        String account = request.getPhone();
        if (account == null || account.isBlank()) {
            account = request.getAccount();
        }
        if (account == null || account.isBlank()) {
            account = request.getEmail();
        }
        return account == null ? null : account.trim();
    }

    private AuthUserDTO toAuthUser(User user, HttpSession session) {
        String id = user == null || user.getId() == null ? "" : String.valueOf(user.getId());
        String email = user == null ? "" : user.getEmail();
        String name = user == null ? "" : (user.getNickname() == null || user.getNickname().isBlank() ? email : user.getNickname());
        String token = "session-" + session.getId() + "-" + UUID.randomUUID();
        return new AuthUserDTO(id, email, name, token);
    }
}
