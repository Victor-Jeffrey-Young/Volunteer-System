package com.volunteer.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.service.SysActivityService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 活动管理模块全生命周期集成测试 (修正 URL 与断言版本)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysActivityService activityService;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
        when(claims.get("userId")).thenReturn(1L);
        when(claims.get("role")).thenReturn("ADMIN");
    }

    @Test
    @DisplayName("场景1：活动发布 - 验证管理员权限下的创建逻辑")
    void addActivity_AdminSuccess() throws Exception {
        SysActivity activity = new SysActivity();
        activity.setTitle("社区大清扫");
        activity.setCapacity(50);

        mockMvc.perform(post("/api/activity/add") // 🚨 修正 URL
                .header("Authorization", "admin-token")
                .header("Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(activityService, times(1)).save(any(SysActivity.class));
    }

    @Test
    @DisplayName("场景2：越权删除拦截 - 志愿者 Token 被 AdminInterceptor 拦下")
    void deleteActivity_Forbidden() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
        when(claims.get("role")).thenReturn("VOLUNTEER");

        mockMvc.perform(delete("/api/activity/100")
                .header("Authorization", "volunteer-token")
                // 伪造管理员角色头：修复后该请求头不再被任何代码读取
                .header("Role", "ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(activityService, never()).removeById(anyLong());
    }

    @Test
    @DisplayName("场景3：活动更新 - 验证通用修改逻辑")
    void updateActivity_Success() throws Exception {
        SysActivity activity = new SysActivity();
        activity.setActivityId(50L);
        activity.setStatus(1); // 设为进行中

        mockMvc.perform(put("/api/activity/update") // 🚨 修正 URL
                .header("Authorization", "admin-token")
                .header("Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(activityService, times(1)).updateById(any(SysActivity.class));
    }

    @Test
    @DisplayName("场景3b：活动更新 - 客户端提交的 currentNum 必须被白名单丢弃")
    void updateActivity_ignoresClientSuppliedCurrentNum() throws Exception {
        SysActivity staleForm = new SysActivity();
        staleForm.setActivityId(50L);
        staleForm.setTitle("旧表单里的标题");
        staleForm.setCapacity(5);
        staleForm.setCurrentNum(0);   // 打开弹窗时的旧快照，真实值已被并发报名改成 1
        staleForm.setStatus(0);

        mockMvc.perform(put("/api/activity/update")
                .header("Authorization", "admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        org.mockito.ArgumentCaptor<SysActivity> captor = org.mockito.ArgumentCaptor.forClass(SysActivity.class);
        verify(activityService, times(1)).updateById(captor.capture());

        SysActivity written = captor.getValue();
        assertEquals(50L, written.getActivityId());
        assertEquals("旧表单里的标题", written.getTitle());
        // 派生计数不在可写字段里
        assertNull(written.getCurrentNum(), "currentNum 不允许被客户端写入");
        assertNull(written.getCreateTime(), "createTime 同样不在白名单里");
    }
}
