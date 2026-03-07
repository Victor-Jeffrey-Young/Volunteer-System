package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.entity.SysActivity;
import com.volunteer.volunteersystem.entity.SysUser;
import com.volunteer.volunteersystem.service.SysActivityService;
import com.volunteer.volunteersystem.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysActivityService activityService;

    @GetMapping("/base")
    public Result<Map<String, Object>> getBaseData() {
        Map<String, Object> data = new HashMap<>();

        // 1. 志愿者总数
        long volCount = userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "VOLUNTEER"));

        // 2. 进行中的活动数 (状态 0 和 1)
        long activeCount = activityService.count(new LambdaQueryWrapper<SysActivity>().in(SysActivity::getStatus, 0, 1));

        // 3. 累计总时长
        List<SysUser> users = userService.list(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "VOLUNTEER"));
        BigDecimal totalHours = users.stream().map(SysUser::getTotalHours).reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("volCount", volCount);
        data.put("activeCount", activeCount);
        data.put("totalHours", totalHours);

        return Result.success(data);
    }

    // E chart 面板
    // 1. 获取活动类型占比 (饼图数据)
    @GetMapping("/typePie")
    public Result<List<Map<String, Object>>> getTypePie() {
        // SQL: SELECT type AS name, COUNT(*) AS value FROM sys_activity GROUP BY type
        QueryWrapper<SysActivity> query = new QueryWrapper<>();
        query.select("type as name", "count(*) as value").groupBy("type");
        List<Map<String, Object>> list = activityService.listMaps(query);
        return Result.success(list);
    }

    // 2. 获取志愿者时长排行榜 Top 5 (柱状图数据)
    @GetMapping("/rank")
    public Result<List<Map<String, Object>>> getRank() {
        // SQL: SELECT real_name AS name, total_hours AS value FROM sys_user WHERE role='VOLUNTEER' ORDER BY total_hours DESC LIMIT 5
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.select("real_name as name", "total_hours as value")
                .eq("role", "VOLUNTEER")
                .orderByDesc("total_hours")
                .last("LIMIT 5"); // 仅取前5名
        List<Map<String, Object>> list = userService.listMaps(query);
        return Result.success(list);
    }

    // 3. 获取近半年每月活动发布趋势 (折线图数据)
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend() {
        // 利用 MySQL 的 DATE_FORMAT 函数按月份分组统计
        // SQL: SELECT DATE_FORMAT(create_time, '%Y-%m') as month, COUNT(*) as count FROM sys_activity GROUP BY month ORDER BY month ASC LIMIT 6
        QueryWrapper<SysActivity> query = new QueryWrapper<>();
        query.select("DATE_FORMAT(create_time, '%Y-%m') as month", "count(*) as count")
                .groupBy("month")
                .orderByAsc("month")
                .last("LIMIT 6");
        List<Map<String, Object>> list = activityService.listMaps(query);
        return Result.success(list);
    }

    // 4. 获取志愿者风采排行榜 (修复版)
    @GetMapping("/volunteer/rank")
    public Result<List<Map<String, Object>>> getVolunteerRank(@RequestParam String type) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();

        // 🚨 核心修复点：
        // 1. 必须查 'total_points' (因为数据库里points字段没了)
        // 2. 必须加上 'as points' (起别名)，这样前端 user.points 才能拿到值！
        query.select("user_id", "username", "real_name", "avatar", "total_hours", "total_points as points")
                .eq("role", "VOLUNTEER")
                .eq("status", 1);

        if ("hours".equals(type)) {
            query.orderByDesc("total_hours");
        } else {
            // 这里排序也要用新的字段名 total_points
            query.orderByDesc("total_points");
        }

        query.last("LIMIT 10");

        List<Map<String, Object>> list = userService.listMaps(query);
        return Result.success(list);
    }
}
