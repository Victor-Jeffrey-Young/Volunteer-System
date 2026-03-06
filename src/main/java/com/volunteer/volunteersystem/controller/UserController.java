package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.entity.SysUser;
import com.volunteer.volunteersystem.service.SysUserService;
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
    @GetMapping("/page")
    public Result<Page<SysUser>> getPage(
            @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) { // 🚨 1. 必须在这里接收前端传来的 name 参数

        // 权限判定逻辑
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "权限不足，非法操作！");
        }

        Page<SysUser> pageInfo = new Page<>(current, size);

        // 🚨 2. 构造 Mybatis-Plus 查询条件包装器
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 如果前端传了 name 且不为空，则拼接 SQL：WHERE real_name LIKE '%name%'
        // 注意：这里是对真实姓名(realName)进行搜索，如果你想按账号(username)搜索，改成 SysUser::getUsername 即可
        wrapper.like(StringUtils.hasText(name), SysUser::getRealName, name);

        // 建议加上：按创建时间倒序，让新注册的用户排在最前面
        wrapper.orderByDesc(SysUser::getCreateTime);

        // 🚨 3. 将 wrapper 传入 page 方法中！
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

//    /**
//     * 修改个人资料
//     */
//    @PutMapping("/update")
//    public Result<String> updateProfile(@RequestBody SysUser user) {
//        userService.updateById(user);
//        return Result.success("个人资料修改成功");
//    }

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


}