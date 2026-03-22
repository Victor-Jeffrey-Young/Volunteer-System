package com.volunteer.system.controller;

import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysGoodsService;
import com.volunteer.system.service.SysWishService;
import com.volunteer.system.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 最终补全测试 (修复版)：涵盖商城管理、心愿初审、活动复杂查询
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdminFinalAuditTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private SysGoodsService goodsService;
    @MockBean private SysWishService wishService;
    @MockBean private SysActivityService activityService;
    @MockBean private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
        when(claims.get("role")).thenReturn("ADMIN");
    }

    @Test
    @DisplayName("补全1：商城管理 - 验证商品删除路径")
    void deleteGoods_Admin() throws Exception {
        // 🚨 修正：URL 路径应为 /api/shop/admin/{id} 而不是 /admin/goods/{id}
        mockMvc.perform(delete("/api/shop/admin/1")
                .header("Authorization", "valid-token")
                .header("Role", "ADMIN"))
                .andExpect(status().isOk());
        verify(goodsService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("补全2：心愿初审 - 验证状态机切换")
    void auditWish_Success() throws Exception {
        SysWish wish = new SysWish();
        wish.setWishId(300L);
        wish.setStatus(0);

        when(wishService.getById(300L)).thenReturn(wish);

        mockMvc.perform(put("/api/wish/admin/audit")
                .header("Authorization", "valid-token")
                .param("wishId", "300")
                .param("status", "1"))
                .andExpect(status().isOk());

        if (wish.getStatus() != 1) throw new AssertionError("状态未翻转");
        verify(wishService, times(1)).updateById(wish);
    }

    @Test
    @DisplayName("补全3：活动多条件查询 - 验证身份认证")
    void getActivityPage_ComplexQuery() throws Exception {
        mockMvc.perform(get("/api/activity/page")
                .header("Authorization", "valid-token") // 🚨 补全认证头
                .param("current", "1")
                .param("size", "5"))
                .andExpect(status().isOk());

        verify(activityService, times(1)).page(any(), any());
    }
}
