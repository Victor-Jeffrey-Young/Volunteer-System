package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j // 启用日志
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser login(String username, String password) {
        log.info("开始执行登录业务逻辑，账号: {}", username);

        // 构造查询条件：SELECT * FROM sys_user WHERE username = ?
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);

        SysUser user = this.getOne(wrapper);

        if (user == null) {
            log.warn("登录失败：账号不存在 ({})", username);
            throw new RuntimeException("账号不存在！");
        }

        // 注意：目前为了测试使用明文比对。后续引入 Spring Security 时，这里会换成 BCrypt 加密比对。
        if (!user.getPassword().equals(password)) {
            log.warn("登录失败：密码错误 ({})", username);
            throw new RuntimeException("密码错误！");
        }

        if (user.getStatus() == 0) {
            log.warn("登录失败：账号被封禁 ({})", username);
            throw new RuntimeException("账号已被封禁，请联系管理员！");
        }

        log.info("登录成功：用户 {}", user.getRealName());
        return user;
    }

    @Override
    public void register(SysUser user) {
        log.info("开始执行注册业务逻辑，账号: {}", user.getUsername());

        // 1. 检查账号是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (this.count(wrapper) > 0) {
            log.warn("注册失败：账号已被占用 ({})", user.getUsername());
            throw new RuntimeException("该账号已被注册！");
        }

        // 2. 设置默认值
        user.setRole("VOLUNTEER"); // 默认角色为普通志愿者
        user.setStatus(1);         // 状态正常
        user.setTotalHours(new java.math.BigDecimal("0.00"));
        user.setCurrentPoints(0);
        user.setTotalPoints(0);

        // 3. 插入数据库
        this.save(user);
        log.info("注册成功，生成的新用户ID为: {}", user.getUserId());
    }
}