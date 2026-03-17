package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 分页查询公告列表
     * 所有人（包含未登录游客，如果前端开放）均可查看，按发布时间倒序排列。
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取公告列表", description = "支持按标题模糊搜索，自动关联发布人姓名")
    @Parameters({
            @Parameter(name = "current", description = "当前页码", example = "1"),
            @Parameter(name = "size", description = "每页展示数量", example = "10"),
            @Parameter(name = "title", description = "搜索关键词(公告标题)")
    })
    public Result<Page<SysNotice>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {

        // 1. 构造 MyBatis-Plus 分页对象
        Page<SysNotice> pageInfo = new Page<>(current, size);

        // 2. 构造查询条件
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), SysNotice::getTitle, title)
                .orderByDesc(SysNotice::getCreateTime);

        // 3. 执行查询
        noticeService.page(pageInfo, wrapper);

        // 4. 业务层数据组装：由于数据库仅存了 publisherId，需连表查询组装发布人的真实姓名
        // 注意：此处在大量数据下可能存在 N+1 性能隐患，但考虑到公告列表通常单页数据极少(如 5-10条)，此种写法更易于维护。
        for (SysNotice notice : pageInfo.getRecords()) {
            SysUser user = userService.getById(notice.getPublisherId());
            if (user != null) {
                notice.setPublisherName(user.getRealName());
            } else {
                notice.setPublisherName("系统管理员"); // 兜底处理
            }
        }

        return Result.success(pageInfo);
    }

    /**
     * 新增公告
     * 仅具备 ADMIN 角色的用户可调用，系统会自动记录当前服务器时间为发布时间。
     */
    @PostMapping("/add")
    @Operation(summary = "[Admin] 发布新公告", description = "管理员专用，需在 Header 携带 Role: ADMIN")
    public Result<String> addNotice(
            @RequestBody SysNotice notice,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {

        if (!"ADMIN".equals(role)) {
            throw new ServiceException(403, "权限不足，仅管理员可发布公告");
        }

        notice.setCreateTime(LocalDateTime.now());
        noticeService.save(notice);
        log.info("管理员发布了新公告: {}", notice.getTitle());

        return Result.success("发布成功");
    }

    /**
     * 修改公告信息
     */
    @PutMapping("/update")
    @Operation(summary = "[Admin] 编辑公告", description = "修改已有公告的标题、正文或类型")
    public Result<String> updateNotice(
            @RequestBody SysNotice notice,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {

        if (!"ADMIN".equals(role)) {
            throw new ServiceException(403, "权限不足，无权修改公告");
        }

        noticeService.updateById(notice);
        return Result.success("修改成功");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] 物理删除公告", description = "根据 ID 永久删除该条记录")
    public Result<String> deleteNotice(
            @Parameter(description = "要删除的公告ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {

        if (!"ADMIN".equals(role)) {
            throw new ServiceException(403, "权限不足，无权删除公告");
        }

        noticeService.removeById(id);
        log.warn("管理员删除了公告, ID: {}", id);

        return Result.success("删除成功");
    }
}