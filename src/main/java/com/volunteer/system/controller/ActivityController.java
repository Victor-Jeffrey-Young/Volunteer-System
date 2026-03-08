package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.service.SysActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private SysActivityService activityService;

    // 1. 分页查询活动列表 (所有角色可见)
    @GetMapping("/page")
    public Result<Page<SysActivity>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {

        Page<SysActivity> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), SysActivity::getTitle, title);
        wrapper.orderByDesc(SysActivity::getCreateTime);

        activityService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    // 2. 新增活动 (管理员权限校验)
    @PostMapping("/add")
    public Result<String> addActivity(@RequestBody SysActivity activity, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "只有管理员可以发布活动");
        }
        activity.setCurrentNum(0); // 初始化报名人数为0
        activity.setStatus(0);     // 默认招募中
        activityService.save(activity);
        return Result.success("活动发布成功");
    }

    // 3. 修改活动
    @PutMapping("/update")
    public Result<String> updateActivity(@RequestBody SysActivity activity, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "无权修改");
        activityService.updateById(activity);
        return Result.success("修改成功");
    }

    // 4. 删除活动
    @DeleteMapping("/{id}")
    public Result<String> deleteActivity(@PathVariable Long id, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "无权删除");
        activityService.removeById(id);
        return Result.success("删除成功");
    }
}