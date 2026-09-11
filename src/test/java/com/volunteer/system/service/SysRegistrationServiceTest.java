package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysRegistration;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysRegistrationMapper;
import com.volunteer.system.service.impl.SysRegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动报名与工时结算核心逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
public class SysRegistrationServiceTest {

    @Mock private SysRegistrationMapper registrationMapper;
    @Mock private SysActivityService activityService;
    @Mock private SysUserService userService;

    @InjectMocks private SysRegistrationServiceImpl registrationService;

    private SysUser mockUser;
    private SysActivity mockActivity;
    private SysRegistration mockReg;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 初始化用户
        mockUser = new SysUser();
        mockUser.setUserId(1L);
        mockUser.setTotalHours(BigDecimal.ZERO);
        mockUser.setTotalPoints(0);
        mockUser.setCurrentPoints(0);

        // 2. 初始化活动
        mockActivity = new SysActivity();
        mockActivity.setActivityId(10L);
        mockActivity.setStatus(0); // 招募中
        mockActivity.setCapacity(10);
        mockActivity.setCurrentNum(5);
        mockActivity.setStartTime(LocalDateTime.now().plusMinutes(10));

        // 3. 初始化报名记录
        mockReg = new SysRegistration();
        mockReg.setRegId(50L);
        mockReg.setUserId(1L);
        mockReg.setActivityId(10L);
        mockReg.setStatus(1); // 已审核通过

        // 4. 反射注入 baseMapper
        Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(registrationService, registrationMapper);
    }

    @Test
    @DisplayName("场景1：申请报名 - 成功并增加名额")
    void applyActivity_Success() {
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        // 模拟没有重复报名
        when(registrationMapper.selectCount(any())).thenReturn(0L);

        registrationService.applyActivity(1L, 10L);

        // 验证活动人数增加了 1
        assertEquals(6, mockActivity.getCurrentNum());
        // 验证保存了报名记录
        verify(registrationMapper, times(1)).insert(any(SysRegistration.class));
        verify(activityService, times(1)).updateById(mockActivity);
    }

    @Test
    @DisplayName("场景2：申请报名 - 名额已满拦截 (400)")
    void applyActivity_Full() {
        mockActivity.setCurrentNum(10); // 已满
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            registrationService.applyActivity(1L, 10L);
        });

        assertEquals(400, ex.getCode());
        assertEquals("名额已满！", ex.getMessage());
    }

    @Test
    @DisplayName("场景2b：申请报名 - 已有签退待结算记录(status=6)时拦截 (409)")
    void applyActivity_DuplicateStatus6() {
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        // 模拟已存在一条 status=6 (已签退待结算) 的有效报名
        when(registrationMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            registrationService.applyActivity(1L, 10L);
        });

        assertEquals(409, ex.getCode());
        // 不应插入任何记录，也不应增加名额
        verify(registrationMapper, never()).insert(any(SysRegistration.class));
        verify(activityService, never()).updateById(any(SysActivity.class));
    }

    @Test
    @DisplayName("场景2c：申请报名 - 唯一索引兜底冲突时转为 409")
    void applyActivity_DuplicateKeyFallback() {
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        // 判重通过 (count=0)，但插入时撞上数据库唯一索引
        when(registrationMapper.selectCount(any())).thenReturn(0L);
        when(registrationMapper.insert(any(SysRegistration.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '1-10-1' for key 'uk_user_activity_active'"));

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            registrationService.applyActivity(1L, 10L);
        });

        assertEquals(409, ex.getCode());
        // 插入失败后事务应回滚，不再更新活动人数
        verify(activityService, never()).updateById(any(SysActivity.class));
    }

    @Test
    @DisplayName("场景3：签到打卡 - 成功改变状态")
    void signIn_Success() {
        mockActivity.setStatus(1); // 活动设为进行中
        // mock 内部调用 getById
        // 注意：ServiceImpl 的 getById 也会调用 baseMapper
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getById(10L)).thenReturn(mockActivity);

        registrationService.signIn(50L, 1L);

        assertEquals(5, mockReg.getStatus()); // 状态应变为 5 (已签到)
        assertNotNull(mockReg.getSignInTime());
        verify(registrationMapper, times(1)).updateById(mockReg);
    }

    @Test
    @DisplayName("场景5：取消报名 - 验证状态流转与名额释放")
    void cancelRegistration_Success() {
        // 模拟已审核通过状态
        mockReg.setStatus(1);
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);

        registrationService.cancelRegistration(50L, 1L);

        // 状态应变为 4 (已取消)
        assertEquals(4, mockReg.getStatus());
        // 活动名额应释放 (5 - 1 = 4)
        assertEquals(4, mockActivity.getCurrentNum());
        
        verify(registrationMapper, times(1)).updateById(mockReg);
        verify(activityService, times(1)).updateById(mockActivity);
    }

    @Test
    @DisplayName("场景6：管理员审核拒绝 - 验证名额自动退回")
    void auditRegistration_Reject() {
        mockReg.setStatus(0); // 待审核
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);

        registrationService.auditRegistration(50L, 2, "不符合要求");

        // 状态应变为 2 (拒绝)
        assertEquals(2, mockReg.getStatus());
        assertEquals("不符合要求", mockReg.getRemarks());
        // 名额释放
        assertEquals(4, mockActivity.getCurrentNum());
    }
}
