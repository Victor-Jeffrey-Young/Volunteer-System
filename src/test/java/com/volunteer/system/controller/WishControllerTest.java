package com.volunteer.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.SysWishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 邻里微心愿业务流程 Controller 层集成测试
 * 验证业务 API 的请求响应、状态流转及点赞结算逻辑
 */
@SpringBootTest
@AutoConfigureMockMvc
public class WishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysWishService wishService;

    @MockBean
    private SysUserService userService;

    @MockBean
    private com.volunteer.system.utils.JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private SysWish mockWish;
    private SysUser mockVolunteer;

    @BeforeEach
    void setUp() {
        // 模拟 JWT 校验过程，使其对任意 Token 均返回有效的 Claims
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
        when(claims.get("userId")).thenReturn(10L);
        when(claims.get("role")).thenReturn("ADMIN");

        mockWish = new SysWish();
        mockWish.setWishId(200L);
        mockWish.setRequesterId(5L);
        mockWish.setVolunteerId(10L);
        mockWish.setStatus(5); // 待确认状态
        mockWish.setIsLiked(0);

        mockVolunteer = new SysUser();
        mockVolunteer.setUserId(10L);
        mockVolunteer.setLikes(5); // 初始有5个赞
        mockVolunteer.setCurrentPoints(100);
        mockVolunteer.setTotalHours(new BigDecimal("10.0"));
    }

    @Test
    @DisplayName("场景1：心愿确认完成并点赞 - 验证志愿者获赞数自增")
    void confirmWishWithLike() throws Exception {
        // 模拟 Service 查询
        when(wishService.getById(200L)).thenReturn(mockWish);
        when(userService.getById(10L)).thenReturn(mockVolunteer);

        // 发起请求，带上 Mock 的 Token
        mockMvc.perform(put("/api/wish/confirm")
                .header("Authorization", "any-token") // 💡 触发拦截器逻辑
                .param("wishId", "200")
                .param("liked", "true")
                .param("rateMsg", "非常感谢！"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证业务结果：志愿者点赞数从 5 变成 6
        assertEquals(6, mockVolunteer.getLikes());
        assertEquals(1, mockWish.getIsLiked());

        // 验证数据库更新调用
        verify(userService, times(1)).updateById(mockVolunteer);
        verify(wishService, times(1)).updateById(mockWish);
    }

    @Test
    @DisplayName("场景2：管理员结算心愿 - 验证工时积分注入志愿者账户")
    void settleWish() throws Exception {
        // 模拟状态为 6 (待结算)
        mockWish.setStatus(6);
        when(wishService.getById(200L)).thenReturn(mockWish);
        when(userService.getById(10L)).thenReturn(mockVolunteer);

        // 构造结算数据
        SysWish settleData = new SysWish();
        settleData.setWishId(200L);
        settleData.setRewardPoints(50);
        settleData.setRewardHours(2.0);

        mockMvc.perform(post("/api/wish/admin/settle")
                .header("Authorization", "any-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settleData)))
                .andExpect(status().isOk());

        // 验证账户变化
        // 100 + 50 = 150 积分
        assertEquals(150, mockVolunteer.getCurrentPoints());
        // 10.0 + 2.0 = 12.0 工时
        assertEquals(new BigDecimal("12.0"), mockVolunteer.getTotalHours());

        verify(userService, times(1)).updateById(mockVolunteer);
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("预期 " + expected + " 但实际是 " + actual);
        }
    }
}
