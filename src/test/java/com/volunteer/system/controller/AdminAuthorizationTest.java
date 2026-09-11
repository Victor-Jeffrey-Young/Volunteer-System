package com.volunteer.system.controller;

import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理员接口授权回归测试 —— 本项目最重要的一组安全测试。
 *
 * 与其它 Controller 测试的关键区别：**JwtUtils 是真实的，没有 @MockBean**。
 * 之前的测试把 JwtUtils mock 成恒返回 ADMIN，等于把鉴权整条链路短路掉了，
 * 所以「伪造 Role 请求头能否提权」这个真实漏洞在 44 个用例里一个都测不出来。
 *
 * 这里用真实签名的 Token 走完整的拦截器链，覆盖四个断言：
 *   1. 无 Token                    → 401（认证失败）
 *   2. 志愿者 Token                → 403（授权失败）
 *   3. 志愿者 Token + 伪造 Role 头 → 403（**核心回归：请求头不再被信任**）
 *   4. 管理员 Token（哪怕伪造 Role: VOLUNTEER）→ 200（角色只认 JWT）
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdminAuthorizationTest {

    private static final String ADMIN_ENDPOINT = "/api/activity/add";

    @Autowired
    private MockMvc mockMvc;

    /** 真实工具类：负责签发与验签，保证测试走的是生产同一条鉴权链路 */
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SysActivityService activityService;

    private String body() throws Exception {
        SysActivity activity = new SysActivity();
        activity.setTitle("授权回归测试活动");
        activity.setCapacity(10);
        return objectMapper.writeValueAsString(activity);
    }

    @Test
    @DisplayName("无 Token 访问管理接口 → 401 未认证")
    void noToken_IsUnauthorized() throws Exception {
        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(activityService, never()).save(any(SysActivity.class));
    }

    @Test
    @DisplayName("志愿者 Token 访问管理接口 → 403 权限不足")
    void volunteerToken_IsForbidden() throws Exception {
        String volunteerToken = jwtUtils.createToken(10L, "VOLUNTEER");

        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .header("Authorization", volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(activityService, never()).save(any(SysActivity.class));
    }

    @Test
    @DisplayName("核心回归：志愿者 Token + 伪造 Role: ADMIN 请求头 → 仍然 403")
    void forgedRoleHeader_CannotEscalatePrivilege() throws Exception {
        String volunteerToken = jwtUtils.createToken(10L, "VOLUNTEER");

        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .header("Authorization", volunteerToken)
                        // 旧实现正是读这个头来判断角色，攻击者随手就能改成 ADMIN
                        .header("Role", "ADMIN")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        // 关键断言：不仅返回码正确，而且业务方法根本没被调用（数据没被篡改）
        verify(activityService, never()).save(any(SysActivity.class));
    }

    @Test
    @DisplayName("管理员 Token 正常放行；伪造 Role: VOLUNTEER 也无法降级绕过")
    void adminToken_IsAllowed_RoleComesFromJwtOnly() throws Exception {
        String adminToken = jwtUtils.createToken(1L, "ADMIN");

        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .header("Authorization", adminToken)
                        .header("Role", "VOLUNTEER")   // 客户端说什么不算数
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(activityService).save(any(SysActivity.class));
    }

    @Test
    @DisplayName("Bearer 前缀与裸 Token 两种写法都能通过认证")
    void bearerPrefix_IsTolerated() throws Exception {
        String adminToken = jwtUtils.createToken(1L, "ADMIN");

        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("被篡改签名的 Token 一律 401")
    void tamperedToken_IsUnauthorized() throws Exception {
        String adminToken = jwtUtils.createToken(1L, "ADMIN");
        // 改掉签名末尾一个字符，验签必须失败
        String tampered = adminToken.substring(0, adminToken.length() - 1)
                + (adminToken.endsWith("A") ? "B" : "A");

        mockMvc.perform(post(ADMIN_ENDPOINT)
                        .header("Authorization", tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isUnauthorized());

        verify(activityService, never()).save(any(SysActivity.class));
    }
}
