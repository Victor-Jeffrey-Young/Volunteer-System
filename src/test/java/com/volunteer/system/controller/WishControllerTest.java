package com.volunteer.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.SysWishService;
import com.volunteer.system.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 微心愿接口的「身份来源」回归测试。
 *
 * 这一组用例专门守护水平越权（IDOR）修复：Controller 必须把 JWT 里的 userId 传给 Service，
 * 而客户端在 URL 参数或请求体里塞的任何 userId / requesterId 都必须被忽略。
 * 与 AdminAuthorizationTest 一样，JwtUtils 使用真实实现，Token 是真实签发的。
 */
@SpringBootTest
@AutoConfigureMockMvc
public class WishControllerTest {

    private static final long JWT_VOLUNTEER_ID = 10L;
    private static final long JWT_RESIDENT_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SysWishService wishService;

    @MockBean
    private SysUserService userService;

    @BeforeEach
    void setUp() {
        // 心愿池等读接口会遍历 Service 返回的列表，默认给空列表避免 NPE
        org.mockito.Mockito.lenient().when(
                wishService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of());
    }

    private String volunteerToken() {
        return jwtUtils.createToken(JWT_VOLUNTEER_ID, "VOLUNTEER");
    }

    private String residentToken() {
        return jwtUtils.createToken(JWT_RESIDENT_ID, "RESIDENT");
    }

    @Test
    @DisplayName("认领心愿：Service 收到的 userId 来自 JWT")
    void claim_UsesIdentityFromJwt() throws Exception {
        mockMvc.perform(put("/api/wish/claim")
                        .header("Authorization", volunteerToken())
                        .param("wishId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(wishService).claimWish(200L, JWT_VOLUNTEER_ID);
    }

    @Test
    @DisplayName("IDOR 回归：URL 上额外传 userId 也无法冒充他人认领")
    void claim_IgnoresClientSuppliedUserId() throws Exception {
        mockMvc.perform(put("/api/wish/claim")
                        .header("Authorization", volunteerToken())
                        .param("wishId", "200")
                        .param("userId", "999")        // 攻击者试图冒用他人身份
                        .param("volunteerId", "999"))
                .andExpect(status().isOk());

        // 传给 Service 的仍然是 JWT 里的 10，而不是 999
        verify(wishService).claimWish(200L, JWT_VOLUNTEER_ID);
    }

    @Test
    @DisplayName("标记完成：Service 收到的 userId 来自 JWT")
    void finish_UsesIdentityFromJwt() throws Exception {
        mockMvc.perform(put("/api/wish/finish")
                        .header("Authorization", volunteerToken())
                        .param("wishId", "200"))
                .andExpect(status().isOk());

        verify(wishService).finishWish(200L, JWT_VOLUNTEER_ID);
    }

    @Test
    @DisplayName("确认完成并点赞：Service 收到的 userId 来自 JWT")
    void confirm_UsesIdentityFromJwt() throws Exception {
        mockMvc.perform(put("/api/wish/confirm")
                        .header("Authorization", residentToken())
                        .param("wishId", "200")
                        .param("liked", "true")
                        .param("rateMsg", "非常感谢！"))
                .andExpect(status().isOk());

        verify(wishService).confirmWish(200L, JWT_RESIDENT_ID, true, "非常感谢！");
    }

    @Test
    @DisplayName("发布心愿：requesterId 由 Service 从 JWT 注入，请求体里的同名字段无效")
    void apply_RequesterIdComesFromJwt() throws Exception {
        SysWish forged = new SysWish();
        forged.setTitle("伪造发布者身份的心愿");
        forged.setContent("尝试把 requesterId 写成别人");
        forged.setRequesterId(999L);   // 攻击载荷

        mockMvc.perform(post("/api/wish/apply")
                        .header("Authorization", residentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forged)))
                .andExpect(status().isOk());

        // Controller 只透传 JWT 身份，真正覆盖 requesterId 的逻辑在 Service 里
        verify(wishService).applyWish(any(SysWish.class), eq(JWT_RESIDENT_ID));
    }

    @Test
    @DisplayName("删除心愿：Service 收到的 userId 来自 JWT")
    void deleteWish_UsesIdentityFromJwt() throws Exception {
        mockMvc.perform(delete("/api/wish/200")
                        .header("Authorization", residentToken()))
                .andExpect(status().isOk());

        verify(wishService).deleteRejectedWish(200L, JWT_RESIDENT_ID);
    }

    @Test
    @DisplayName("管理员结算接口：志愿者调用被 AdminInterceptor 拦下，Service 不会被触达")
    void settle_RequiresAdmin() throws Exception {
        SysWish settleData = new SysWish();
        settleData.setWishId(200L);
        settleData.setRewardPoints(50);
        settleData.setRewardHours(2.0);

        mockMvc.perform(post("/api/wish/admin/settle")
                        .header("Authorization", volunteerToken())
                        .header("Role", "ADMIN")     // 伪造头也救不了
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settleData)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(wishService, org.mockito.Mockito.never())
                .settleWish(any(), any(), any());
    }

    @Test
    @DisplayName("心愿池是公开接口：任何登录用户都能浏览")
    void pool_IsAccessibleToLoggedInUsers() throws Exception {
        mockMvc.perform(get("/api/wish/pool")
                        .header("Authorization", volunteerToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("未登录访问心愿池 → 401")
    void pool_RequiresLogin() throws Exception {
        mockMvc.perform(get("/api/wish/pool"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("用户密码哈希不会随接口泄漏（WRITE_ONLY 生效）")
    void passwordIsNeverSerialized() throws Exception {
        SysUser user = new SysUser();
        user.setUserId(5L);
        user.setUsername("resident");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV");
        user.setRealName("测试居民");

        String json = objectMapper.writeValueAsString(user);

        org.junit.jupiter.api.Assertions.assertFalse(json.contains("password"),
                "序列化结果不应包含 password 字段，实际为: " + json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("resident"));
    }
}
