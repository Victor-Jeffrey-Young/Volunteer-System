package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 真实数据库登录接口
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginForm) {
        log.info("接收到登录请求: {}", loginForm.getUsername());
        try {
            // 调用 Service 层的登录逻辑
            SysUser user = sysUserService.login(loginForm.getUsername(), loginForm.getPassword());

            // 登录成功，生成 Token (临时用 UUID 代替，后续换成真 JWT)
            String token = UUID.randomUUID().toString().replace("-", "");

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("username", user.getUsername()); // 🚨 必须返回账号，用于生成固定头像
            data.put("role", user.getRole()); // 🚨 检查这里：user.getRole() 是否有值？
            data.put("userId", user.getUserId()); //
            data.put("totalPoints", user.getTotalPoints());
            data.put("currentPoints", user.getCurrentPoints());
            data.put("avatar", user.getAvatar());     // 🚨 返回数据库存的头像
            data.put("token", token);

            return Result.success(data);
        } catch (RuntimeException e) {
            // 捕获 Service 层抛出的异常（如密码错误）
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 志愿者注册接口
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody SysUser registerForm) {
        log.info("接收到注册请求: {}", registerForm.getUsername());
        try {
            sysUserService.register(registerForm);
            return Result.success("注册成功，请前往登录");
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }
    }
}