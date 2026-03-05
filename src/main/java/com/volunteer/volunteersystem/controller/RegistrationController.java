package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.entity.SysActivity;
import com.volunteer.volunteersystem.entity.SysRegistration;
import com.volunteer.volunteersystem.entity.SysUser;
import com.volunteer.volunteersystem.service.SysActivityService;
import com.volunteer.volunteersystem.service.SysRegistrationService;
import com.volunteer.volunteersystem.service.SysUserService;
import com.volunteer.volunteersystem.service.impl.SysRegistrationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reg")
public class RegistrationController {

    @Autowired
    private SysRegistrationService registrationService;

    @Autowired
    private SysActivityService activityService;

    @Autowired
    private SysUserService userService;


    // 1. 志愿者报名接口
    @PostMapping("/apply")
    public Result<String> apply(@RequestParam Long userId, @RequestParam Long activityId) {
        try {
            registrationService.applyActivity(userId, activityId);
            return Result.success("报名成功，等待管理员审核！");
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 2. 查询我报名的活动列表 (个人中心使用)
    @GetMapping("/my")
    public Result<List<SysRegistration>> getMyRecords(@RequestParam Long userId) {
        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegistration::getUserId, userId).orderByDesc(SysRegistration::getApplyTime);

        List<SysRegistration> list = registrationService.list(wrapper);

        for (SysRegistration reg : list) {
            SysActivity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                reg.setActivityTitle(activity.getTitle());
                reg.setActivityLocation(activity.getLocation());
                // 🚨 新增：填充活动状态和时间
                reg.setActivityStatus(activity.getStatus());
                reg.setActivityStartTime(activity.getStartTime());
            } else {
                reg.setActivityTitle("【活动已失效】");
            }
        }
        return Result.success(list);
    }

    // 3. 分页查询：管理员查询报名记录
    // 3. 管理员查询报名记录 (带分页与状态筛选)
    @GetMapping("/admin/page")
    public Result<Page<SysRegistration>> getAdminPage(
            @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) { // 🚨 新增：接收前端传来的状态参数

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        Page<SysRegistration> pageInfo = new Page<>(current, size);

        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();

        // 🚨 核心筛选逻辑：如果前端传了 status 且不为空，则拼接到 WHERE 条件中
        if (status != null) {
            wrapper.eq(SysRegistration::getStatus, status);
        }

        wrapper.orderByDesc(SysRegistration::getApplyTime);

        registrationService.page(pageInfo, wrapper);

        // 组装活动标题和志愿者姓名 (保持不变)
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

    // 4. 管理员审核接口
    @PutMapping("/admin/audit")
    public Result<String> audit(@RequestParam Long regId, @RequestParam Integer status, @RequestParam(required = false) String remarks) {
        try {
            // 调用刚写的 Service 方法
            ((SysRegistrationServiceImpl)registrationService).auditRegistration(regId, status, remarks);
            return Result.success("审核操作成功");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 5. 管理员发放工时接口
    @PostMapping("/admin/grant")
    public Result<String> grantHours(@RequestParam Long regId, @RequestParam BigDecimal actualHours) {
        try {
            ((SysRegistrationServiceImpl)registrationService).grantHours(regId, actualHours);
            return Result.success("工时发放成功！");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 取消报名
    @PutMapping("/cancel")
    public Result<String> cancel(@RequestParam Long regId, @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).cancelRegistration(regId, userId);
            return Result.success("已成功取消报名");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // 志愿者签到
    @PutMapping("/sign")
    public Result<String> signIn(@RequestParam Long regId, @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).signIn(regId, userId);
            return Result.success("签到成功！等待管理员发放工时。");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
    // 志愿者签退
    @PutMapping("/sign-out")
    public Result<String> signOut(@RequestParam Long regId, @RequestParam Long userId) {
        try {
            ((SysRegistrationServiceImpl)registrationService).signOut(regId, userId);
            return Result.success("签退成功！辛苦了，请等待管理员核实工时。");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取指定活动的报名人员名单 (管理员审核用)
     * URL 示例: /api/reg/activity/101
     */
    @GetMapping("/admin/activity/{activityId}")
    public Result<List<SysRegistration>> getApplicantsByActivity(
            @PathVariable Long activityId,
            @RequestHeader("Role") String role) {

        // 1. 权限校验
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "权限不足");
        }

        // 2. 构造查询条件：根据活动ID查询
        LambdaQueryWrapper<SysRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegistration::getActivityId, activityId)
                .orderByDesc(SysRegistration::getApplyTime);

        List<SysRegistration> list = registrationService.list(wrapper);

        // 3. 关联志愿者信息 (手动组装或使用复杂的 Join，这里采用手动组装更清晰)
        for (SysRegistration reg : list) {
            SysUser user = userService.getById(reg.getUserId());
            if (user != null) {
                reg.setRealName(user.getRealName()); // 我们在 Entity 里已经加了这个字段
                // 如果需要手机号，可以在 Entity 加字段并在这里 setPhone
            }
        }

        return Result.success(list);
    }


}