package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysNotice;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysNoticeService;
import com.volunteer.system.service.SysUserService;
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
 * 社区新闻与公告控制器
 * 负责处理系统中所有公告内容的CRUD操作及分页展示。
 */
@Slf4j
@RestController
@RequestMapping("/api/notice")
@Tag(name = "08. 新闻公告模块", description = "社区最新动态与通知的发布管理")
public class NoticeController {

    @Autowired
    private SysNoticeService noticeService;
    @Autowired
    private SysUserService userService;


    @GetMapping("/page")
    @Operation(summary = "分页查询公告", description = "企业级实现：自动识别搜索关键词与用户阅读状态")
    public Result<IPage<SysNotice>> getPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "公告标题搜索") @RequestParam(required = false) String title,
            @RequestAttribute("userId") Long userId) { // userId 取自 JWT，前端注入的 Header 一律忽略

        log.info("查询公告列表 - 用户: {}, 搜索词: {}", userId, title);

        IPage<SysNotice> page = new Page<>(current, size);
        IPage<SysNotice> result = noticeService.getNoticePage(page, userId, title);

        return Result.success(result);
    }

    /**
     * 新增公告
     * 仅具备 ADMIN 角色的用户可调用，系统会自动记录当前服务器时间为发布时间。
     */
    @PostMapping("/add")
    @RequiresAdmin
    @Operation(summary = "[Admin] 发布新公告", description = "管理员专用，由 AdminInterceptor 统一鉴权")
    public Result<String> addNotice(@RequestBody SysNotice notice) {

        notice.setCreateTime(LocalDateTime.now());
        noticeService.save(notice);
        log.info("管理员发布了新公告: {}", notice.getTitle());

        return Result.success("发布成功");
    }

    /**
     * 修改公告信息
     */
    @PutMapping("/update")
    @RequiresAdmin
    @Operation(summary = "[Admin] 编辑公告", description = "修改已有公告的标题、正文或类型")
    public Result<String> updateNotice(@RequestBody SysNotice notice) {

        noticeService.updateById(notice);
        return Result.success("修改成功");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @RequiresAdmin
    @Operation(summary = "[Admin] 物理删除公告", description = "根据 ID 永久删除该条记录")
    public Result<String> deleteNotice(
            @Parameter(description = "要删除的公告ID") @PathVariable Long id) {

        noticeService.removeById(id);
        log.warn("管理员删除了公告, ID: {}", id);

        return Result.success("删除成功");
    }

    /**
     * 标记公告为已读
     */
    @PostMapping("/read/{id}")
    @Operation(summary = "标记单条公告为已读", description = "用户点击查看详情后触发，用于消除红点通知")
    public Result<String> markAsRead(
            @Parameter(description = "公告ID", required = true) @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        // 向 sys_notice_read 插入一条记录（如果已存在则忽略）
        noticeService.markAsRead(userId, id);
        return Result.success("已读");
    }

    /**
     * 一键全部标记为已读
     */
    @PostMapping("/read-all")
    @Operation(summary = "全部标记为已读", description = "用于一键清理所有未读通知红点")
    public Result<String> markAllAsRead(
            @RequestAttribute("userId") Long userId) {
        // 将所有未读公告 ID 批量插入 sys_notice_read
        noticeService.markAllAsRead(userId);
        return Result.success("全部已读");
    }

}