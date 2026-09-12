package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.service.SysActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 志愿服务活动管理控制器
 * 负责活动的发布、查询、修改、删除等全生命周期管理。
 */
@Slf4j
@RestController
@RequestMapping("/api/activity")
@Tag(name = "02. 志愿活动模块", description = "活动大厅展示与后台CRUD管理")
public class ActivityController {

    @Autowired
    private SysActivityService activityService;

    /**
     * 分页查询活动列表 (通用接口)
     * 此接口供志愿者在活动大厅浏览，也供管理员在后台查看。
     * 默认按发布时间倒序排列。
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取活动列表", description = "支持按活动标题进行模糊搜索")
    @Parameters({
            @Parameter(name = "current", description = "当前页码", example = "1"),
            @Parameter(name = "size", description = "每页数量", example = "10"),
            @Parameter(name = "title", description = "搜索关键词(活动标题)")
    })
    public Result<Page<SysActivity>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        log.debug("开始分页查询活动, current={}, size={}, title={}", current, size, title);
        // 1. 构造分页对象
        Page<SysActivity> pageInfo = new Page<>(current, size);
        // 2. 构造查询条件
        LambdaQueryWrapper<SysActivity> wrapper = new LambdaQueryWrapper<>();
        // 动态拼接 LIKE 查询
        wrapper.like(StringUtils.hasText(title), SysActivity::getTitle, title);
        wrapper.eq(StringUtils.hasText(type), SysActivity::getType, type);
        wrapper.eq(status != null, SysActivity::getStatus, status);
        // 按发布时间倒序排列，新活动在前
        wrapper.orderByDesc(SysActivity::getCreateTime);
        // 3. 执行 MyBatis-Plus 的物理分页查询
        activityService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 新增活动 (管理员权限)
     * 后端会自动初始化报名人数为 0，状态为“招募中”，并记录发布时间。
     */
    @PostMapping("/add")
    @RequiresAdmin
    @Operation(summary = "[Admin] 发布新活动", description = "仅管理员可调用（由 AdminInterceptor 统一鉴权）")
    public Result<String> addActivity(@RequestBody SysActivity activity) {

        // 初始化业务字段
        activity.setCurrentNum(0); // 初始报名人数
        activity.setStatus(0);     // 0-招募中
        activity.setCreateTime(LocalDateTime.now()); // 记录发布时间

        activityService.save(activity);
        log.info("管理员发布新活动成功，活动ID: {}", activity.getActivityId());

        return Result.success("活动发布成功");
    }

    /**
     * 修改活动信息 (管理员权限)
     * 常用于调整活动详情，或手动变更活动状态（如：招募中 -> 进行中）。
     *
     * 采用白名单更新：只允许改「活动自身的属性」，current_num 不在其中。
     * 它是报名链路（行锁 + 原子语句）维护的派生计数，一旦允许客户端整体写回，
     * 管理员拿旧表单点保存就会把并发期间的报名数覆盖掉
     * （实测：真实有效报名数 1，提交带旧 currentNum=0 的表单后计数变成 0），
     * 而满员判断正是 current_num >= capacity，计数被改小等于放开超额报名。
     */
    @PutMapping("/update")
    @RequiresAdmin
    @Operation(summary = "[Admin] 修改活动信息", description = "仅管理员可调用（由 AdminInterceptor 统一鉴权）；报名人数字段不接受客户端写入")
    public Result<String> updateActivity(@RequestBody SysActivity activity) {

        if (activity.getActivityId() == null) {
            throw new ServiceException(400, "活动ID不能为空");
        }
        if (activity.getCapacity() != null && activity.getCapacity() < 1) {
            throw new ServiceException(400, "招募人数必须大于 0");
        }

        SysActivity updateEntity = new SysActivity();
        updateEntity.setActivityId(activity.getActivityId());
        updateEntity.setTitle(activity.getTitle());
        updateEntity.setContent(activity.getContent());
        updateEntity.setType(activity.getType());
        updateEntity.setLocation(activity.getLocation());
        updateEntity.setStartTime(activity.getStartTime());
        updateEntity.setEndTime(activity.getEndTime());
        updateEntity.setCapacity(activity.getCapacity());
        updateEntity.setRewardHours(activity.getRewardHours());
        updateEntity.setStatus(activity.getStatus());
        updateEntity.setRequiredSkills(activity.getRequiredSkills());

        activityService.updateById(updateEntity);
        log.info("活动信息被修改，活动ID: {}", activity.getActivityId());

        return Result.success("修改成功");
    }

    /**
     * 删除活动 (管理员权限)
     * 注意：这是一个物理删除，关联的报名记录会成为“孤儿记录”。
     */
    @DeleteMapping("/{id}")
    @RequiresAdmin
    @Operation(summary = "[Admin] 删除活动", description = "物理删除，请谨慎操作")
    public Result<String> deleteActivity(
            @Parameter(description = "要删除的活动ID") @PathVariable Long id) {

        activityService.removeById(id);
        log.warn("活动被物理删除，活动ID: {}", id);

        return Result.success("删除成功");
    }
}