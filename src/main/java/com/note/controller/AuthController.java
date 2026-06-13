package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.Result;
import com.note.entity.SysLoginLog;
import com.note.entity.SysUser;
import com.note.entity.dto.LoginRequest;
import com.note.entity.dto.LoginResponse;
import com.note.entity.dto.RegisterRequest;
import com.note.security.JwtUtils;
import com.note.service.SysLoginLogService;
import com.note.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysLoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String browser = httpRequest.getHeader("User-Agent");

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            SysUser sysUser = sysUserService.getOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, userDetails.getUsername()));

            String token = jwtUtils.generateToken(sysUser.getId(), sysUser.getUsername(), sysUser.getRole());

            recordLoginLog(sysUser.getId(), sysUser.getUsername(), ip, browser, 1, "登录成功");

            LoginResponse resp = new LoginResponse(
                    token, sysUser.getId(), sysUser.getUsername(),
                    sysUser.getNickname(), sysUser.getRole());
            return Result.ok(resp);

        } catch (BadCredentialsException e) {
            recordLoginLog(null, request.getUsername(), ip, browser, 0, "用户名或密码错误");
            return Result.fail(401, "用户名或密码错误");
        } catch (DisabledException e) {
            recordLoginLog(null, request.getUsername(), ip, browser, 0, "账号已被禁用");
            return Result.fail(403, "账号已被禁用，请联系管理员");
        }
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        long count = sysUserService.count(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            return Result.fail("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("user");
        user.setStatus(1);
        user.setIsVip(0);
        sysUserService.save(user);
        return Result.ok();
    }

    private void recordLoginLog(Long userId, String username, String ip, String browser,
                                 int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddr(ip);
        log.setBrowser(browser);
        log.setLoginStatus(status);
        log.setMsg(msg);
        loginLogService.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "";
    }
}
