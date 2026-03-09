package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证与基础模块控制器
 * 负责处理用户的登录、注册等无需 Token 拦截的公开接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "01. 认证模块", description = "处理用户登录、注册与身份签发") // 🚨 Knife4j 左侧菜单分类名
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 核心登录接口
     * 业务流：接收前端账号密码 -> 查库比对 -> 校验状态 -> 签发 Token 并返回全量基础信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证账号密码，登录成功后返回 Token 及个人基础信息") // 🚨 Knife4j 接口名
    public Result<Map<String, Object>> login(
            @Parameter(description = "登录表单信息(需包含 username 和 password)") // 🚨 Knife4j 参数说明
            @RequestBody SysUser loginForm) {

        log.info("接收到登录请求: {}", loginForm.getUsername());

        try {
            // 1. 调用 Service 层的登录业务逻辑 (包含密码比对和状态校验)
            SysUser user = sysUserService.login(loginForm.getUsername(), loginForm.getPassword());

            // 2. 签发 Token (目前使用 UUID 模拟，TODO 2.0: 替换为标准 JWT 生成逻辑)
            String token = UUID.randomUUID().toString().replace("-", "");

            // 3. 封装返回给前端的状态流数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            data.put("totalPoints", user.getTotalPoints());     // 决定荣誉段位
            data.put("currentPoints", user.getCurrentPoints()); // 决定商城购买力
            data.put("avatar", user.getAvatar());

            return Result.success(data);
        } catch (RuntimeException e) {
            // 捕获 Service 层抛出的异常（如：密码错误、账号被封禁）
            log.warn("用户登录失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 志愿者注册接口
     * 业务流：接收基础资料 -> 校验账号唯一性 -> 赋予默认角色(VOLUNTEER)和默认积分(0) -> 落库
     */
    @PostMapping("/register")
    @Operation(summary = "志愿者注册", description = "新用户注册，默认赋予 VOLUNTEER 角色")
    public Result<String> register(
            @Parameter(description = "注册信息(需包含 username, password, realName)")
            @RequestBody SysUser registerForm) {

        log.info("接收到注册请求: {}", registerForm.getUsername());

        try {
            // 注册逻辑封装在 Service 层，保证 Controller 层的整洁
            sysUserService.register(registerForm);
            return Result.success("注册成功，请前往登录");
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }
    }
}