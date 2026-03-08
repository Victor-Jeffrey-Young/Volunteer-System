package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserService userService;

    /**
     * 分页查询用户列表 (管理员使用)
     * URL 示例: /api/user/page?current=1&size=10&name=张三
     */
    // 分页查询用户列表 (支持多维筛选)
    @GetMapping("/page")
    public Result<Page<SysUser>> getPage(
            @RequestHeader("Role") String roleHeader, // 鉴权用
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,   // 姓名或账号关键词
            @RequestParam(required = false) String role,   // 筛选角色
            @RequestParam(required = false) Integer status // 筛选状态
    ) {
        if (!"ADMIN".equals(roleHeader)) return Result.error(403, "权限不足");

        Page<SysUser> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 1. 角色筛选 (如果不为空)
        wrapper.eq(StringUtils.hasText(role), SysUser::getRole, role);

        // 2. 状态筛选 (如果不为空)
        wrapper.eq(status != null, SysUser::getStatus, status);

        // 3. 智能模糊搜索：输入关键词，同时匹配【账号】或【真实姓名】
        // SQL效果: AND (real_name LIKE '%xxx%' OR username LIKE '%xxx%')
        if (StringUtils.hasText(name)) {
            wrapper.and(w -> w.like(SysUser::getRealName, name)
                    .or()
                    .like(SysUser::getUsername, name));
        }

        // 4. 排序：管理员排前面，同角色按注册时间倒序
        wrapper.orderByAsc(SysUser::getRole)
                .orderByDesc(SysUser::getCreateTime);

        userService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 修改用户状态 (启用/禁用)
     */
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestBody SysUser user) {
        // 只需要 userId 和 status 两个字段
        userService.updateById(user);
        return Result.success("状态更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("用户删除成功");
    }

    /**
     * 获取当前登录用户详细信息 (用于前端个人中心回显)
     */
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestParam Long userId) {
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 🚨 论文亮点：数据脱敏，不在网络中传输密码
        }
        return Result.success(user);
    }

    /**
     * 修改个人基本信息 (志愿者/管理员通用)
     */
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return Result.error(400, "用户ID不能为空");
        }

        // 🚨 论文亮点：字段级更新限制。
        // 我们不能直接 userService.updateById(user); 否则恶意用户可能通过抓包修改自己的 totalHours 和 points！
        // 必须 new 一个新对象，只把允许修改的字段 set 进去。
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(user.getUserId());
        updateEntity.setRealName(user.getRealName());
        updateEntity.setPhone(user.getPhone());
        updateEntity.setGender(user.getGender());

        // 🚨 新增：允许修改邮箱和头像
        updateEntity.setEmail(user.getEmail());
        updateEntity.setAvatar(user.getAvatar());
        updateEntity.setSkills(user.getSkills());
        updateEntity.setAvailableTime(user.getAvailableTime());
        updateEntity.setEmail(user.getEmail());


        userService.updateById(updateEntity);
        return Result.success("个人资料修改成功");
    }

    /**
     * 修改密码 (通用)
     */
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        Long userId = Long.valueOf(params.get("userId"));
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 校验原密码
        if (!user.getPassword().equals(oldPassword)) {
            return Result.error(400, "原密码错误，修改失败");
        }

        // 更新新密码
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setPassword(newPassword);
        userService.updateById(updateEntity);

        return Result.success("密码修改成功，请重新登录");
    }

    /**
     * 管理员重置指定用户密码为默认值 123456
     */
    @PutMapping("/reset-pwd/{id}")
    public Result<String> resetPassword(@PathVariable Long id, @RequestHeader("Role") String role) {
        // 安全校验：只有管理员可以重置别人密码
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "权限不足，仅管理员可执行此操作");
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(id);
        updateEntity.setPassword("123456"); // 设置默认密码

        userService.updateById(updateEntity);
        return Result.success("密码已成功重置为：123456");
    }


}