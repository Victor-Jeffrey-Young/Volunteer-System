package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j // 启用日志
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser login(String username, String password) {
        log.info("开始执行登录业务逻辑，账号: {}", username);

        // 1. 构造查询条件
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);

        SysUser user = this.getOne(wrapper);

        // 2. 账号存在性校验
        if (user == null) {
            log.warn("登录失败：账号不存在 ({})", username);
            // 对外提示统称为“账号或密码错误”，防止黑客“撞库”枚举出系统中存在的账号
            throw new ServiceException(400, "账号或密码错误！");
        }

        // 3. 密码校验：兼容 BCrypt 与旧 MD5，拒绝明文及未知格式
        if (!PasswordUtils.matches(password, user.getPassword())) {
            log.warn("登录失败：密码错误 ({})", username);
            throw new ServiceException(400, "账号或密码错误！");
        }

        // 4. 状态校验 (增加 != null 判断，防止自动拆箱抛出 NullPointerException)
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("登录失败：账号被封禁 ({})", username);
            throw new ServiceException(403, "账号已被封禁，请联系管理员！");
        }

        // 旧 MD5 用户验证成功后立即升级为 BCrypt，不影响本次登录
        if (PasswordUtils.isLegacyMd5(user.getPassword())) {
            String oldPassword = user.getPassword();
            String upgradedPassword = PasswordUtils.encode(password);
            int updated = baseMapper.updatePasswordIfCurrent(
                    user.getUserId(), oldPassword, upgradedPassword);
            if (updated != 1) {
                log.warn("登录失败：密码升级期间账号状态已变化 ({})", username);
                throw new ServiceException(400, "账号或密码错误！");
            }
            user.setPassword(upgradedPassword);
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
            throw new ServiceException(409, "该账号已被注册！");
        }

        // 2. 新注册密码统一使用 BCrypt 保存
        user.setPassword(PasswordUtils.encode(user.getPassword()));

        // 3. 设置角色与状态
        // 允许前端传入角色（如 RESIDENT），如果是空或尝试注册为 ADMIN，则强制回归 VOLUNTEER
        if (user.getRole() == null || user.getRole().trim().isEmpty() || "ADMIN".equals(user.getRole())) {
            user.setRole("VOLUNTEER");
        }

        user.setStatus(1);                                   // 状态正常
        user.setTotalHours(new BigDecimal("0.00"));     // 规范的 BigDecimal 赋值方式
        user.setCurrentPoints(0);
        user.setTotalPoints(0);

        // 4. 插入数据库
        // 上面的 count 判重只在单线程成立：两个并发注册都能查到 0 条。
        // 真正的兜底是 sys_user.username 上的唯一索引，冲突时把它翻译成友好提示，
        // 而不是让 DatabaseException 冒泡成 500。
        try {
            this.save(user);
        } catch (DuplicateKeyException e) {
            log.warn("注册失败：账号并发重复 ({})", user.getUsername());
            throw new ServiceException(409, "该账号已被注册！");
        }
        log.info("注册成功，生成的新用户ID为: {}", user.getUserId());
    }

    @Override
    public int addRewards(Long userId, int points, BigDecimal hours) {
        return baseMapper.addRewards(userId, points, hours == null ? BigDecimal.ZERO : hours);
    }

    @Override
    public int deductPointsIfEnough(Long userId, int points) {
        return baseMapper.deductPointsIfEnough(userId, points);
    }

    @Override
    public int incrementLikes(Long userId) {
        return baseMapper.incrementLikes(userId);
    }
}
