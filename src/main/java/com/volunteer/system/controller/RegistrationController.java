package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysRegistration;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysRegistrationService;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.impl.SysRegistrationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报名与工时闭环管理控制器
 * 负责处理活动报名、审核、O2O双重打卡及事务级工时结算的全部核心业务流。
 */
@Slf4j
@RestController
@RequestMapping("/api/reg")
@Tag(name = "04. 报名与工时模块", description = "志愿者报名、双重打卡及管理员审核与工时结算")
public class RegistrationController {

    @Autowired
    private SysRegistrationService registrationService;

    @Autowired
    private SysActivityService activityService;

    @Autowired
    private SysUserService userService;


    /**
     * 志愿者提交活动报名申请
     */
    @PostMapping("/apply")
    @Operation(summary = "提交报名申请", description = "后端会校验防超卖并发、以及是否重复报名")
    public Result<String> apply(
            @Parameter(description = "志愿者用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "报名的活动ID", required = true) @RequestParam Long activityId) {
        log.info("接收到报名请求 - 用户ID: {}, 活动ID: {}", userId, activityId);
        try {
            registrationService.applyActivity(userId, activityId);
            return Result.success("报名成功，等待管理员审核！");
        } catch (RuntimeException e) {
            log.warn("报名失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 志愿者取消报名
     */
    @PutMapping("/cancel")
    @Operation(summary = "主动取消报名", description = "仅在待审核或已通过状态下允许取消，并释放名额")
    public Result<String> cancel(
            @Parameter(description = "报名记录ID", required = true) @RequestParam Long regId,
            @Parameter(description = "志愿者用户ID(安全校验)", required = true) @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).cancelRegistration(regId, userId);
            return Result.success("已成功取消报名");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 志愿者查询自己的历史报名记录 (个人中心使用)
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的报名记录", description = "按申请时间倒序排列，包含关联的活动状态")
    public Result<List<SysRegistration>> getMyRecords(
            @Parameter(description = "志愿者用户ID", required = true) @RequestParam Long userId) {

        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegistration::getUserId, userId).orderByDesc(SysRegistration::getApplyTime);

        List<SysRegistration> list = registrationService.list(wrapper);

        // 连表组装活动信息，用于前端判断是否允许打卡
        for (SysRegistration reg : list) {
            SysActivity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                reg.setActivityTitle(activity.getTitle());
                reg.setActivityLocation(activity.getLocation());
                reg.setActivityStatus(activity.getStatus());
                reg.setActivityStartTime(activity.getStartTime());
            } else {
                reg.setActivityTitle("【活动已失效】");
            }
        }
        return Result.success(list);
    }

    // ==========================================
    // 📱 O2O 双重打卡模块
    // ==========================================

    /**
     * 志愿者现场扫码签到
     */
    @PutMapping("/sign")
    @Operation(summary = "现场扫码签到", description = "校验活动必须为进行中状态，记录 signInTime")
    public Result<String> signIn(
            @Parameter(description = "报名记录ID", required = true) @RequestParam Long regId,
            @Parameter(description = "志愿者用户ID", required = true) @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).signIn(regId, userId);
            log.info("签到成功 - 记录ID: {}", regId);
            return Result.success("签到成功！等待管理员发放工时。");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 志愿者现场扫码签退
     */
    @PutMapping("/sign-out")
    @Operation(summary = "现场扫码签退", description = "服务结束打卡，记录 signOutTime")
    public Result<String> signOut(
            @Parameter(description = "报名记录ID", required = true) @RequestParam Long regId,
            @Parameter(description = "志愿者用户ID", required = true) @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).signOut(regId, userId);
            log.info("签退成功 - 记录ID: {}", regId);
            return Result.success("签退成功！辛苦了，请等待管理员核实工时。");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ==========================================
    // 🛡️ 管理员后台审核与结算模块
    // ==========================================

    /**
     * 管理员分页查询所有报名记录 (支持多维筛选)
     */
    @GetMapping("/admin/page")
    @Operation(summary = "[Admin] 分页查询全局报名流水", description = "支持按状态筛选，包含用户信息和活动信息")
    @Parameters({
            @Parameter(name = "current", description = "当前页码", example = "1"),
            @Parameter(name = "size", description = "每页展示数量", example = "10"),
            @Parameter(name = "status", description = "流转状态过滤(如: 0-待审, 6-已签退)")
    })
    public Result<Page<SysRegistration>> getAdminPage(
            @Parameter(hidden = true) @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        Page<SysRegistration> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(SysRegistration::getStatus, status);
        }
        wrapper.orderByDesc(SysRegistration::getApplyTime);

        registrationService.page(pageInfo, wrapper);

        // 组装连表展示数据
        for (SysRegistration reg : pageInfo.getRecords()) {
            SysActivity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                reg.setActivityTitle(activity.getTitle());
                reg.setActivityLocation(activity.getLocation());
            } else {
                reg.setActivityTitle("【该活动已下架或删除】");
                reg.setActivityLocation("--");
            }

            SysUser user = userService.getById(reg.getUserId());
            if (user != null) reg.setRealName(user.getRealName());
            else reg.setRealName("【用户已注销】");
        }

        return Result.success(pageInfo);
    }

    /**
     * 获取指定活动的报名人员名单 (用于活动管理页面的穿透查询)
     */
    @GetMapping("/admin/activity/{activityId}")
    @Operation(summary = "[Admin] 获取单项活动名单", description = "查询指定活动的报名人员列表(含历史被拒/取消记录)")
    public Result<List<SysRegistration>> getApplicantsByActivity(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegistration::getActivityId, activityId)
                .orderByDesc(SysRegistration::getApplyTime);

        List<SysRegistration> list = registrationService.list(wrapper);

        for (SysRegistration reg : list) {
            SysUser user = userService.getById(reg.getUserId());
            if (user != null) {
                reg.setRealName(user.getRealName());
            }
        }
        return Result.success(list);
    }

    /**
     * 管理员审核报名申请
     */
    @PutMapping("/admin/audit")
    @Operation(summary = "[Admin] 审核报名申请", description = "操作状态机流转 (1-通过, 2-拒绝)")
    @Parameters({
            @Parameter(name = "regId", description = "报名记录ID", required = true),
            @Parameter(name = "status", description = "目标状态: 1-通过, 2-拒绝", required = true),
            @Parameter(name = "remarks", description = "拒绝时的反馈理由")
    })
    public Result<String> audit(
            @RequestParam Long regId,
            @RequestParam Integer status,
            @RequestParam(required = false) String remarks) {
        try {
            ((SysRegistrationServiceImpl)registrationService).auditRegistration(regId, status, remarks);
            return Result.success("审核操作成功");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 管理员发放工时与双轨积分 (核心闭环)
     */
    @PostMapping("/admin/grant")
    @Operation(summary = "[Admin] 一键结算工时(支持事务)", description = "结算后自动增加用户的累计时长及双轨积分，状态变为已完结")
    public Result<String> grantHours(
            @Parameter(description = "报名记录ID", required = true) @RequestParam Long regId,
            @Parameter(description = "最终核发的小时数", required = true) @RequestParam BigDecimal actualHours) {
        try {
            ((SysRegistrationServiceImpl)registrationService).grantHours(regId, actualHours);
            log.info("工时结算成功 - 记录ID: {}, 发放工时: {}h", regId, actualHours);
            return Result.success("工时发放成功！");
        } catch (Exception e) {
            log.error("工时结算失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }
}