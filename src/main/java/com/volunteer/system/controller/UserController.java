package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.utils.PasswordUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户权限与个人中心控制器
 * 负责全系统用户的状态管理、多维检索以及志愿者个人画像的维护。
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "03. 用户管理模块", description = "管理员进行用户权限管控，以及用户个人的资料与密码维护")
public class UserController {

    @Autowired
    private SysUserService userService;

    // ==========================================
    // 管理员：用户状态与权限控制接口
    // ==========================================

    @GetMapping("/page")
    @RequiresAdmin
    @Operation(summary = "[Admin] 分页与多维筛选查询用户", description = "利用动态SQL，支持按角色、状态过滤及按姓名/账号模糊检索")
    @Parameters({
            @Parameter(name = "current", description = "当前页码", example = "1"),
            @Parameter(name = "size", description = "每页展示数量", example = "10"),
            @Parameter(name = "name", description = "智能模糊搜索(匹配姓名或账号)"),
            @Parameter(name = "role", description = "角色类型(ADMIN/VOLUNTEER)"),
            @Parameter(name = "status", description = "账号状态(1-正常, 0-封禁)")
    })
    public Result<Page<SysUser>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status
    ) {
        Page<SysUser> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 1. 角色筛选 (如果不为空)
        wrapper.eq(StringUtils.hasText(role), SysUser::getRole, role);

        // 2. 状态筛选 (如果不为空)
        wrapper.eq(status != null, SysUser::getStatus, status);

        // 3. 智能模糊搜索：输入关键词，同时匹配【账号】或【真实姓名】
        // 动态 SQL 构建效果: AND (real_name LIKE '%xxx%' OR username LIKE '%xxx%')
        if (StringUtils.hasText(name)) {
            wrapper.and(w -> w.like(SysUser::getRealName, name)
                    .or()
                    .like(SysUser::getUsername, name));
        }

        // 4. 排序规则：管理员排前面，同角色按注册时间倒序
        wrapper.orderByAsc(SysUser::getRole)
                .orderByDesc(SysUser::getCreateTime);

        userService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    @PutMapping("/status")
    @RequiresAdmin
    @Operation(summary = "[Admin] 切换用户状态", description = "用于封禁违规用户或解除封禁限制；只接受 userId 与 status 两个字段")
    public Result<String> updateStatus(@RequestBody SysUser user) {

        if (user.getUserId() == null) {
            return Result.error(400, "用户ID不能为空");
        }
        if (user.getStatus() == null || (user.getStatus() != 0 && user.getStatus() != 1)) {
            return Result.error(400, "状态只能是 0-封禁 / 1-正常");
        }

        // 查出目标用户信息，防止管理员互相伤害
        SysUser target = userService.getById(user.getUserId());
        if (target == null) {
            return Result.error(404, "用户不存在");
        }
        if ("ADMIN".equals(target.getRole())) {
            return Result.error(403, "权限不足：无法对管理账号进行封禁操作");
        }

        // 白名单更新：只允许改 status。
        // 这里原先是直接把请求体整体 updateById，而这个接口的语义只需要 userId + status：
        //   * 批量赋值：请求体里塞 currentPoints / totalPoints / totalHours / role
        //     都会被一并写库，等于给封禁接口开了改资产、改角色的后门；
        //   * 丢更新：客户端带来的旧积分若与并发结算（addRewards）相遇，
        //     整体写回会把刚发的积分覆盖掉。
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(user.getUserId());
        updateEntity.setStatus(user.getStatus());

        userService.updateById(updateEntity);
        log.info("管理员变更了用户状态, 用户ID: {}, 新状态: {}", user.getUserId(), user.getStatus());
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    @RequiresAdmin
    @Operation(summary = "[Admin] 物理删除用户", description = "警告：此操作不可逆")
    public Result<String> deleteUser(@PathVariable Long id) {

        // 保护管理员账号不被物理删除
        SysUser target = userService.getById(id);
        if (target != null && "ADMIN".equals(target.getRole())) {
            return Result.error(403, "权限不足：无法删除管理员账号");
        }

        userService.removeById(id);
        log.warn("管理员物理删除了用户, 用户ID: {}", id);
        return Result.success("用户删除成功");
    }

    @PutMapping("/reset-pwd/{id}")
    @RequiresAdmin
    @Operation(summary = "[Admin] 强制重置用户密码", description = "将指定用户的密码恢复为默认值 123456")
    public Result<String> resetPassword(
            @Parameter(description = "被重置的用户ID") @PathVariable Long id) {

        // 禁止重置其他管理员的密码
        SysUser target = userService.getById(id);
        if (target != null && "ADMIN".equals(target.getRole())) {
            return Result.error(403, "权限不足：无法重置管理者的密码");
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(id);
        updateEntity.setPassword(PasswordUtils.encode("123456"));

        userService.updateById(updateEntity);
        log.info("管理员重置了用户密码, 目标用户ID: {}", id);
        return Result.success("密码已成功重置为：123456");
    }


    // ==========================================
    // 通用权限：个人中心维护接口
    // ==========================================

    @GetMapping("/info")
    @Operation(summary = "获取当前用户详细信息", description = "用于导航栏和个人中心数据回显，包含数据脱敏处理")
    public Result<SysUser> getUserInfo(
            @RequestAttribute("userId") Long userId) {

        SysUser user = userService.getById(userId);
        if (user != null) {
            // 后端数据脱敏 (Data Masking)。在 JSON 序列化返回前端前，主动擦除密码哈希。
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "修改个人基本资料与技能画像", description = "采用白名单更新策略，防止越权篡改核心资产")
    public Result<String> updateProfile(
            @RequestAttribute("userId") Long userId,
            @RequestBody SysUser user) {
        // 仅允许登录用户维护自己的资料，userId 以 JWT 为准，忽略前端传入
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }

        // 防越权篡改 (Field-level Protection)。
        // 绝对不能直接使用 userService.updateById(user); 否则恶意抓包者可修改 totalPoints 和 totalHours！
        // 必须实例化一个全新的安全沙箱对象，按白名单放行允许修改的字段。
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setRealName(user.getRealName());
        updateEntity.setPhone(user.getPhone());
        updateEntity.setGender(user.getGender());
        updateEntity.setEmail(user.getEmail());
        updateEntity.setAvatar(user.getAvatar());
        updateEntity.setSkills(user.getSkills());
        updateEntity.setAvailableTime(user.getAvailableTime());

        userService.updateById(updateEntity);
        return Result.success("个人资料修改成功");
    }

    @PutMapping("/password")
    @Operation(summary = "用户自主修改密码", description = "需提交并校验原密码")
    public Result<String> updatePassword(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "包含 oldPassword, newPassword 的JSON")
            @RequestBody Map<String, String> params) {

        // userId 以 JWT 为准，跳过前端传入，防止伪造他人身份改密
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 兼容 BCrypt 与旧 MD5 密码校验
        if (!PasswordUtils.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "原密码错误，修改失败");
        }

        // 新密码统一使用 BCrypt 保存
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setPassword(PasswordUtils.encode(newPassword));
        userService.updateById(updateEntity);

        return Result.success("密码修改成功，请重新登录");
    }
}
