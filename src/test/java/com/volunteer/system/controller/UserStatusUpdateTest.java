package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端「切换用户状态」接口的字段白名单测试。
 *
 * 权限本身由 AdminInterceptor 依据 @RequiresAdmin 统一校验，这里只验证业务写路径：
 * 接口语义是 userId + status，请求体里夹带的核心资产字段必须被丢弃。
 */
@ExtendWith(MockitoExtension.class)
class UserStatusUpdateTest {

    @Mock
    private SysUserService userService;

    @InjectMocks
    private UserController controller;

    @Test
    void updateStatus_writesOnlyUserIdAndStatus() {
        when(userService.getById(2L)).thenReturn(user(2L, "VOLUNTEER"));

        // 恶意/误传的请求体：夹带积分、总工时与角色
        SysUser body = new SysUser();
        body.setUserId(2L);
        body.setStatus(0);
        body.setCurrentPoints(999999);
        body.setTotalPoints(999999);
        body.setPassword("hacked");
        body.setRole("ADMIN");

        Result<String> result = controller.updateStatus(body);

        assertEquals(200, result.getCode());

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        SysUser written = captor.getValue();

        assertEquals(2L, written.getUserId());
        assertEquals(0, written.getStatus().intValue());
        assertNull(written.getCurrentPoints(), "积分不允许从状态接口写入");
        assertNull(written.getTotalPoints(), "总积分不允许从状态接口写入");
        assertNull(written.getPassword(), "密码不允许从状态接口写入");
        assertNull(written.getRole(), "角色不允许从状态接口写入");
    }

    @Test
    void updateStatus_rejectsInvalidStatusValue() {
        SysUser body = new SysUser();
        body.setUserId(2L);
        body.setStatus(5);   // 越界状态值

        Result<String> result = controller.updateStatus(body);

        assertEquals(400, result.getCode());
        verify(userService, never()).updateById(any(SysUser.class));
    }

    @Test
    void updateStatus_blocksAdminTarget() {
        when(userService.getById(1L)).thenReturn(user(1L, "ADMIN"));

        SysUser body = new SysUser();
        body.setUserId(1L);
        body.setStatus(0);

        Result<String> result = controller.updateStatus(body);

        assertEquals(403, result.getCode());
        verify(userService, never()).updateById(any(SysUser.class));
    }

    @Test
    void updateStatus_reportsMissingUser() {
        when(userService.getById(99L)).thenReturn(null);

        SysUser body = new SysUser();
        body.setUserId(99L);
        body.setStatus(0);

        Result<String> result = controller.updateStatus(body);

        assertEquals(404, result.getCode());
        verify(userService, never()).updateById(any(SysUser.class));
    }

    private SysUser user(Long id, String role) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUsername("u" + id);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }
}
