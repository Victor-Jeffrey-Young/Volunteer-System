package com.volunteer.system.controller;

import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysUserService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据看板聚合逻辑集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysUserService userService;

    @MockBean
    private SysActivityService activityService;

    @MockBean
    private SysWishService wishService;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // 绕过安全拦截器
        Claims claims = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
        when(claims.get("userId")).thenReturn(1L);
        when(claims.get("role")).thenReturn("ADMIN");
    }

    @Test
    @DisplayName("场景1：基础数据统计 - 验证总工时内存聚合逻辑")
    void getBaseData_Success() throws Exception {
        // 模拟志愿者总数
        when(userService.count(any())).thenReturn(150L);
        // 模拟活跃活动数
        when(activityService.count(any())).thenReturn(12L);
        
        // 模拟用户列表，用于测试 Service 层中的 Stream 聚合工时逻辑
        List<SysUser> mockUsers = new ArrayList<>();
        SysUser u1 = new SysUser(); u1.setTotalHours(new BigDecimal("10.5"));
        SysUser u2 = new SysUser(); u2.setTotalHours(new BigDecimal("20.0"));
        mockUsers.add(u1);
        mockUsers.add(u2);
        when(userService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockUsers);

        mockMvc.perform(get("/api/dashboard/base")
                .header("Authorization", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.volCount").value(150))
                .andExpect(jsonPath("$.data.activeCount").value(12))
                // 10.5 + 20.0 = 30.5
                .andExpect(jsonPath("$.data.totalHours").value(30.5));
    }

    @Test
    @DisplayName("场景2：ECharts 饼图数据 - 验证 Map 结构转换")
    void getTypePie_Success() throws Exception {
        List<Map<String, Object>> mockList = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "环境保护");
        m1.put("value", 25);
        mockList.add(m1);

        // listMaps 是 MyBatis-Plus 的原生方法，需指定 Wrapper 类型
        when(activityService.listMaps(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockList);

        mockMvc.perform(get("/api/dashboard/typePie")
                .header("Authorization", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("环境保护"))
                .andExpect(jsonPath("$.data[0].value").value(25));
    }

    @Test
    @DisplayName("场景3：志愿者排行 - 验证 TOP 10 返回逻辑")
    void getRank_Success() throws Exception {
        List<Map<String, Object>> mockRank = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "志愿者" + i);
            item.put("value", 100 - i);
            mockRank.add(item);
        }
        when(userService.listMaps(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockRank);

        mockMvc.perform(get("/api/dashboard/rank")
                .header("Authorization", "mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("志愿者1"));
    }
}
