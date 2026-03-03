package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.entity.SysUser;
import com.volunteer.volunteersystem.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("Role") String role, // 临时方案：从请求头获取角色
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        // 🚨 权限判定逻辑
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "权限不足，非法操作！");
        }

        Page<SysUser> pageInfo = new Page<>(current, size);
        userService.page(pageInfo);
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
     * 修改个人资料
     */
    @PutMapping("/update")
    public Result<String> updateProfile(@RequestBody SysUser user) {
        userService.updateById(user);
        return Result.success("个人资料修改成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("用户删除成功");
    }
}