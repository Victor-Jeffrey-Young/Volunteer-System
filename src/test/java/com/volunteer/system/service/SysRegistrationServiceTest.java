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
    @DisplayName("场景3：签到打卡 - 条件更新命中后只写 status 与签到时间")
    void signIn_Success() {
        mockActivity.setStatus(1); // 活动设为进行中
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getById(10L)).thenReturn(mockActivity);
        when(registrationMapper.signInIfApproved(50L, 1L)).thenReturn(1);

        registrationService.signIn(50L, 1L);

        verify(registrationMapper, times(1)).signInIfApproved(50L, 1L);
        // 关键：不再整体写回实体（整体写回会覆盖并发期间管理员写入的工时/积分字段）
        verify(registrationMapper, never()).updateById(any(SysRegistration.class));
    }

    @Test
    @DisplayName("场景3b：并发重复签到 - 条件更新 0 行时必须失败")
    void signIn_AlreadySigned_Rejected() {
        mockActivity.setStatus(1);
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getById(10L)).thenReturn(mockActivity);
        // 模拟第二个并发请求：状态已被别人改走
        when(registrationMapper.signInIfApproved(50L, 1L)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.signIn(50L, 1L));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("场景4：签退打卡 - 条件更新命中后只写 status 与签退时间")
    void signOut_Success() {
        mockReg.setStatus(5); // 已签到
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(registrationMapper.signOutIfSignedIn(50L, 1L)).thenReturn(1);

        registrationService.signOut(50L, 1L);

        verify(registrationMapper, times(1)).signOutIfSignedIn(50L, 1L);
        verify(registrationMapper, never()).updateById(any(SysRegistration.class));
    }

    @Test
    @DisplayName("场景4b：并发重复签退 - 条件更新 0 行时必须失败")
    void signOut_AlreadySignedOut_Rejected() {
        mockReg.setStatus(5);
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(registrationMapper.signOutIfSignedIn(50L, 1L)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.signOut(50L, 1L));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("场景5：取消报名 - 条件更新命中后释放一个名额")
    void cancelRegistration_Success() {
        // 模拟已审核通过状态
        mockReg.setStatus(1);
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        when(registrationMapper.cancelIfActive(50L, 1L)).thenReturn(1);

        registrationService.cancelRegistration(50L, 1L);

        // 状态流转由条件更新完成，且只释放一个名额
        verify(registrationMapper, times(1)).cancelIfActive(50L, 1L);
        verify(activityService, times(1)).releaseSlot(10L);
        // 不再整体写回实体（避免覆盖并发期间的其它写入）
        verify(registrationMapper, never()).updateById(any(SysRegistration.class));
    }

    @Test
    @DisplayName("场景5b：并发/重复取消 - 条件更新 0 行时不得重复释放名额")
    void cancelRegistration_AlreadyCancelled_DoesNotReleaseTwice() {
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        // 模拟第二个并发请求：状态已被别人改走，条件更新命中 0 行
        when(registrationMapper.cancelIfActive(50L, 1L)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.cancelRegistration(50L, 1L));

        assertEquals(400, ex.getCode());
        // 关键断言：没有释放名额
        verify(activityService, never()).releaseSlot(anyLong());
    }

    @Test
    @DisplayName("场景6：管理员审核拒绝 - 条件更新命中后释放一个名额")
    void auditRegistration_Reject() {
        mockReg.setStatus(0); // 待审核
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        when(registrationMapper.auditIfPending(50L, 2, "不符合要求")).thenReturn(1);

        registrationService.auditRegistration(50L, 2, "不符合要求");

        verify(registrationMapper, times(1)).auditIfPending(50L, 2, "不符合要求");
        verify(activityService, times(1)).releaseSlot(10L);
    }

    @Test
    @DisplayName("场景6b：审核状态越界 - 必须拒绝，不允许跳到任意状态")
    void auditRegistration_InvalidStatus() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.auditRegistration(50L, 6, "想直接跳到已签退"));

        assertEquals(400, ex.getCode());
        verify(registrationMapper, never()).auditIfPending(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("场景6c：并发重复审核 - 条件更新 0 行时不得重复释放名额")
    void auditRegistration_AlreadyHandled_DoesNotReleaseTwice() {
        mockReg.setStatus(0);
        when(registrationMapper.selectById(50L)).thenReturn(mockReg);
        when(activityService.getByIdForUpdate(10L)).thenReturn(mockActivity);
        when(registrationMapper.auditIfPending(50L, 2, null)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.auditRegistration(50L, 2, null));

        assertEquals(400, ex.getCode());
        verify(activityService, never()).releaseSlot(anyLong());
    }
}
