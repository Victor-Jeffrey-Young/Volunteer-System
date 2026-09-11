package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.SysWishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板与统计控制器
 * 负责为首页卡片、ECharts数据大屏、荣誉殿堂提供聚合统计数据
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "06. 数据看板模块", description = "大屏可视化图表与基础数据聚合统计")
public class DashboardController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysActivityService activityService;
    @Autowired
    private SysWishService wishService;

    @GetMapping("/base")
    @Operation(summary = "获取首页基础统计数据", description = "统计志愿者总数、进行中的活动数及社区累计志愿总时长")
    public Result<Map<String, Object>> getBaseData() {
        log.debug("正在执行首页基础数据统计分析...");
        Map<String, Object> data = new HashMap<>();

        // 1. 志愿者总数 (条件：role = 'VOLUNTEER')
        long volCount = userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "VOLUNTEER"));

        // 2. 活跃活动数 (状态 0-招募中，1-进行中)
        long activeCount = activityService.count(new LambdaQueryWrapper<SysActivity>().in(SysActivity::getStatus, 0, 1));

        // 3. 累计总时长 (利用 Stream API 在内存中聚合，数据量极大时建议改用 SQL SUM 函数)
        List<SysUser> users = userService.list(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "VOLUNTEER"));
        BigDecimal totalHours = users.stream().map(SysUser::getTotalHours).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 居民点赞总数 (所有志愿者的 likes 总和)
        int totalLikes = users.stream().mapToInt(SysUser::getLikes).sum();

        data.put("volCount", volCount);
        data.put("activeCount", activeCount);
        data.put("totalHours", totalHours);
        data.put("totalLikes", totalLikes);

        return Result.success(data);
    }

    @GetMapping("/typePie")
    @Operation(summary = "获取活动类型占比", description = "利用 SQL GROUP BY 聚合，用于渲染 ECharts 饼状图")
    public Result<List<Map<String, Object>>> getTypePie() {
        QueryWrapper<SysActivity> query = new QueryWrapper<>();
        // SELECT type AS name, COUNT(*) AS value FROM sys_activity GROUP BY type
        query.select("type as name", "count(*) as value").groupBy("type");
        List<Map<String, Object>> list = activityService.listMaps(query);
        return Result.success(list);
    }

    @GetMapping("/rank")
    @Operation(summary = "获取志愿时长 TOP 排行榜", description = "用于渲染 ECharts 柱状图，取前 10 名")
    public Result<List<Map<String, Object>>> getRank() {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        // SELECT real_name AS name, total_hours AS value FROM sys_user WHERE role='VOLUNTEER' ORDER BY total_hours DESC LIMIT 10
        query.select("real_name as name", "total_hours as value")
                .eq("role", "VOLUNTEER")
                .orderByDesc("total_hours")
                .last("LIMIT 10"); // 限制为前10名
        List<Map<String, Object>> list = userService.listMaps(query);
        return Result.success(list);
    }

    @GetMapping("/wishPie")
    @Operation(summary = "获取微心愿分类占比", description = "利用 SQL GROUP BY 聚合，用于渲染 ECharts 饼状图")
    public Result<List<Map<String, Object>>> getWishPie() {
        QueryWrapper<SysWish> query = new QueryWrapper<>();
        query.select("category as name", "count(*) as value").groupBy("category");
        List<Map<String, Object>> list = wishService.listMaps(query);
        return Result.success(list);
    }

    @GetMapping("/likesRank")
    @Operation(summary = "获取志愿者获赞数 TOP 排行榜", description = "用于渲染 ECharts 柱状图，取前 10 名")
    public Result<List<Map<String, Object>>> getLikesRank() {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.select("real_name as name", "likes as value")
                .eq("role", "VOLUNTEER")
                .orderByDesc("likes")
                .last("LIMIT 10"); // 限制为前10名
        List<Map<String, Object>> list = userService.listMaps(query);
        return Result.success(list);
    }

    @GetMapping("/trend")
    @Operation(summary = "获取近半年活动发布趋势", description = "按月聚合统计，用于渲染 ECharts 折线图")
    public Result<List<Map<String, Object>>> getTrend() {
        QueryWrapper<SysActivity> query = new QueryWrapper<>();
        // SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as count FROM sys_activity GROUP BY month ORDER BY month ASC LIMIT 6
        query.select("DATE_FORMAT(create_time, '%Y-%m') as month", "count(*) as count")
                .groupBy("month")
                .orderByAsc("month")
                .last("LIMIT 6");
        List<Map<String, Object>> list = activityService.listMaps(query);
        return Result.success(list);
    }

    @GetMapping("/volunteer/rank")
    @Operation(summary = "获取志愿者排行榜", description = "返回时长或积分前10名的志愿者及趋势数据")
    public Result<List<Map<String, Object>>> getVolunteerRank(
            @Parameter(description = "排行类型：hours (按时长), points (按积分)", required = true, example = "hours")
            @RequestParam String type) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
                query.select("user_id", "username", "real_name", "avatar", "total_hours", "total_points as points", "last_rank")
                         .eq("role", "VOLUNTEER")
                         .eq("status", 1);
                if ("hours".equals(type)) {
                    query.orderByDesc("total_hours");
                } else {
                    query.orderByDesc("total_points");
                }

                query.last("LIMIT 10");
                List<Map<String, Object>> list = userService.listMaps(query);
                return Result.success(list);
    }

    @GetMapping("/volunteers")
    @Operation(summary = "志愿者榜单数据", description = "返回全部活跃志愿者的脱敏数据（不含手机/邮箱/密码），供排行榜实时名次计算，任何登录用户可访问")
    public Result<List<SysUser>> getVolunteersForRank() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, "VOLUNTEER")
                .eq(SysUser::getStatus, 1)
                .orderByDesc(SysUser::getTotalHours)
                .last("LIMIT 500");

        List<SysUser> list = userService.list(wrapper);
        // 榜单仅需展示姓名/头像/积分/时长，脱敏敏感字段
        for (SysUser u : list) {
            u.setPassword(null);
            u.setPhone(null);
            u.setEmail(null);
        }
        return Result.success(list);
    }
}