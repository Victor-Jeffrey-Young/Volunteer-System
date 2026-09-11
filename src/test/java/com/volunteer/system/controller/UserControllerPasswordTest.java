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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerPasswordTest {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private SysUserService userService;

    @InjectMocks
    private UserController controller;

    @Test
    void resetPassword_StoresBcryptInsteadOfPlaintext() {
        SysUser target = user(2L, "VOLUNTEER", PASSWORD_ENCODER.encode("old-password"));
        when(userService.getById(2L)).thenReturn(target);
        when(userService.updateById(any(SysUser.class))).thenReturn(true);

        // 管理员权限已由 AdminInterceptor 依据 @RequiresAdmin 统一校验，Controller 只处理业务
        Result<String> result = controller.resetPassword(2L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        String storedPassword = captor.getValue().getPassword();
        assertNotEquals("123456", storedPassword);
        assertTrue(PASSWORD_ENCODER.matches("123456", storedPassword));
    }

    @Test
    void updatePassword_AcceptsBcryptPasswordAndStoresNewBcryptPassword() {
        SysUser target = user(2L, "VOLUNTEER", PASSWORD_ENCODER.encode("old-password"));
        when(userService.getById(2L)).thenReturn(target);
        when(userService.updateById(any(SysUser.class))).thenReturn(true);

        Result<String> result = controller.updatePassword(2L, Map.of(
                "oldPassword", "old-password",
                "newPassword", "new-password"
        ));

        assertEquals(200, result.getCode());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertTrue(PASSWORD_ENCODER.matches("new-password", captor.getValue().getPassword()));
    }

    @Test
    void updatePassword_AcceptsLegacyMd5PasswordAndStoresBcryptPassword() {
        String legacyMd5 = DigestUtils.md5DigestAsHex("old-password".getBytes(StandardCharsets.UTF_8));
        SysUser target = user(2L, "VOLUNTEER", legacyMd5);
        when(userService.getById(2L)).thenReturn(target);
        when(userService.updateById(any(SysUser.class))).thenReturn(true);

        Result<String> result = controller.updatePassword(2L, Map.of(
                "oldPassword", "old-password",
                "newPassword", "new-password"
        ));

        assertEquals(200, result.getCode());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateById(captor.capture());
        assertTrue(PASSWORD_ENCODER.matches("new-password", captor.getValue().getPassword()));
    }

    @Test
    void updatePassword_RejectsWrongPasswordWithoutWriting() {
        SysUser target = user(2L, "VOLUNTEER", PASSWORD_ENCODER.encode("old-password"));
        when(userService.getById(2L)).thenReturn(target);

        Result<String> result = controller.updatePassword(2L, Map.of(
                "oldPassword", "wrong-password",
                "newPassword", "new-password"
        ));

        assertEquals(400, result.getCode());
        verify(userService, never()).updateById(any(SysUser.class));
    }

    private SysUser user(Long id, String role, String password) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setRole(role);
        user.setPassword(password);
        return user;
    }
}
