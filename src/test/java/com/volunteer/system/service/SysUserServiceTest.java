package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试 (最终完善版)
 */
@ExtendWith(MockitoExtension.class)
public class SysUserServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private SysUserServiceImpl userService;

    private SysUser mockUser;

    @BeforeEach
    void setUp() throws Exception {
        mockUser = new SysUser();
        mockUser.setUserId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("123456");
        mockUser.setRealName("测试用户");
        mockUser.setStatus(1);
        mockUser.setRole("VOLUNTEER");

        // 反射注入 baseMapper
        java.lang.reflect.Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(userService, userMapper);
    }

    @Test
    @DisplayName("场景1：登录成功")
    void login_Success() {
        org.mockito.Mockito.lenient().when(userMapper.selectOne(any())).thenReturn(mockUser);
        org.mockito.Mockito.lenient().when(userMapper.selectOne(any(), anyBoolean())).thenReturn(mockUser);
        SysUser result = userService.login("testuser", "123456");
        assertNotNull(result);
        assertEquals("测试用户", result.getRealName());
    }

    @Test
    @DisplayName("场景2：账号不存在")
    void login_UserNotFound() {
        org.mockito.Mockito.lenient().when(userMapper.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> userService.login("nonexistent", "123456"));
    }

    @Test
    @DisplayName("场景3：密码错误")
    void login_WrongPassword() {
        org.mockito.Mockito.lenient().when(userMapper.selectOne(any())).thenReturn(mockUser);
        assertThrows(ServiceException.class, () -> userService.login("testuser", "wrong"));
    }

    @Test
    @DisplayName("场景4：账号封禁")
    void login_UserBanned() {
        mockUser.setStatus(0);
        org.mockito.Mockito.lenient().when(userMapper.selectOne(any())).thenReturn(mockUser);
        assertThrows(ServiceException.class, () -> userService.login("testuser", "123456"));
    }

    @Test
    @DisplayName("场景5：修改资料")
    void updateProfile_Success() {
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        SysUser up = new SysUser();
        up.setUserId(1L);
        up.setRealName("new");
        userService.updateById(up);
        verify(userMapper, times(1)).updateById(any());
    }

    @Test
    @DisplayName("场景6：用户注册")
    void register_Success() {
        SysUser newUser = new SysUser();
        newUser.setUsername("newboy");
        newUser.setPassword("pwd");

        when(userMapper.selectCount(any())).thenReturn(0L);
        
        // 关键：模拟 MyBatis-Plus 的 insert 行为，直接修改传入的对象
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setUserId(99L); // 手动回填ID
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        userService.register(newUser);

        // 验证：ID 应该已经被 register 方法内部的 insert 逻辑回填
        assertNotNull(newUser.getUserId(), "注册后用户ID不应为空");
        assertEquals(99L, newUser.getUserId());
        assertEquals("VOLUNTEER", newUser.getRole());
    }
}
