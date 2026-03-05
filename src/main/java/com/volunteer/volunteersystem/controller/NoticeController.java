package com.volunteer.volunteersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.volunteersystem.common.Result;
import com.volunteer.volunteersystem.entity.SysNotice;
import com.volunteer.volunteersystem.entity.SysUser;
import com.volunteer.volunteersystem.service.SysNoticeService;
import com.volunteer.volunteersystem.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private SysNoticeService noticeService;
    @Autowired
    private SysUserService userService;

    // 分页查询 (所有人均可查看)
    @GetMapping("/page")
    public Result<Page<SysNotice>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {

        Page<SysNotice> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), SysNotice::getTitle, title)
                .orderByDesc(SysNotice::getCreateTime);

        noticeService.page(pageInfo, wrapper);

        // 组装发布人姓名
        for (SysNotice notice : pageInfo.getRecords()) {
            SysUser user = userService.getById(notice.getPublisherId());
            if (user != null) notice.setPublisherName(user.getRealName());
        }
        return Result.success(pageInfo);
    }

    // 新增公告 (仅管理员)
    @PostMapping("/add")
    public Result<String> addNotice(@RequestBody SysNotice notice, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");
        notice.setCreateTime(LocalDateTime.now());
        noticeService.save(notice);
        return Result.success("发布成功");
    }

    // 修改公告 (仅管理员)
    @PutMapping("/update")
    public Result<String> updateNotice(@RequestBody SysNotice notice, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");
        noticeService.updateById(notice);
        return Result.success("修改成功");
    }

    // 删除公告 (仅管理员)
    @DeleteMapping("/{id}")
    public Result<String> deleteNotice(@PathVariable Long id, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");
        noticeService.removeById(id);
        return Result.success("删除成功");
    }
}
