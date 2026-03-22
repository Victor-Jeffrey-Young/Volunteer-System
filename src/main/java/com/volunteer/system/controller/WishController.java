package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.SysWishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wish")
@Tag(name = "05. 邻里微心愿模块", description = "居民发布求助、志愿者认领与管理员审核心愿")
public class WishController {

    @Autowired
    private SysWishService wishService;

    @Autowired
    private SysUserService userService;

    /**
     * 居民发布新微心愿
     */
    @PostMapping("/apply")
    @Operation(summary = "居民提交心愿", description = "权限: RESIDENT")
    public Result<String> apply(@RequestBody SysWish wish) {
        wish.setStatus(0); // 初始待审核
        wish.setCreateTime(LocalDateTime.now());
        wishService.save(wish);
        return Result.success("心愿提交成功，管理员审核后将发布！");
    }

    /**
     * 志愿者获取心愿池列表 (待认领)
     */
    @GetMapping("/pool")
    @Operation(summary = "心愿池大厅", description = "过滤展示状态为 1-待认领 的心愿")
    public Result<List<SysWish>> getWishPool() {
        LambdaQueryWrapper<SysWish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysWish::getStatus, 1).orderByDesc(SysWish::getCreateTime);
        List<SysWish> list = wishService.list(wrapper);
        
        // 装填发起人信息
        for (SysWish wish : list) {
            SysUser requester = userService.getById(wish.getRequesterId());
            if (requester != null) {
                wish.setRequesterName(requester.getRealName());
                wish.setRequesterPhone(requester.getPhone());
                wish.setRequesterAvatar(requester.getAvatar());
            }
        }
        return Result.success(list);
    }

    /**
     * 志愿者认领心愿
     */
    @PutMapping("/claim")
    @Operation(summary = "认领心愿任务", description = "志愿者将待认领的心愿锁定为办理中状态")
    public Result<String> claim(
            @Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId,
            @Parameter(description = "认领志愿者用户ID", required = true) @RequestParam Long userId) {
        
        SysWish wish = wishService.getById(wishId);
        if (wish == null || wish.getStatus() != 1) {
            return Result.error(500, "该心愿已不可领或已被抢先认领");
        }
        
        wish.setVolunteerId(userId);
        wish.setStatus(2); // 办理中
        wishService.updateById(wish);
        return Result.success("成功认领！请尽快与居民取得联系并提供服务。");
    }

    /**
     * 志愿者标记已完成
     */
    @PutMapping("/finish")
    @Operation(summary = "志愿者标记服务完成", description = "志愿者完成线下服务后，将状态变更为待确认")
    public Result<String> finish(@Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId) {
        SysWish wish = wishService.getById(wishId);
        if (wish == null || wish.getStatus() != 2) {
            return Result.error(500, "当前状态不可标记完成");
        }
        wish.setStatus(5); // 5-已处理待确认
        wishService.updateById(wish);
        return Result.success("服务记录已提交，请等待居民确认。");
    }

    /**
     * 居民点击确认完成
     */
    @PutMapping("/confirm")
    @Operation(summary = "居民确认服务完成", description = "居民对服务结果进行确认，并可进行点赞和评价")
    public Result<String> confirm(
            @Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId,
            @Parameter(description = "是否点赞") @RequestParam(required = false) Boolean liked,
            @Parameter(description = "反馈评价信息") @RequestParam(required = false) String rateMsg) {
        SysWish wish = wishService.getById(wishId);
        if (wish == null || wish.getStatus() != 5) {
            return Result.error(500, "当前状态不可标记确认");
        }
        wish.setStatus(6); // 6-已确认待结算
        
        if (rateMsg != null && !rateMsg.trim().isEmpty()) {
            wish.setRemarks("居民评价: " + rateMsg);
        }
        
        // 如果点了赞且有认领志愿者
        if (Boolean.TRUE.equals(liked) && wish.getVolunteerId() != null) {
             wish.setIsLiked(1); // 记录该笔订单被点过赞
             SysUser volunteer = userService.getById(wish.getVolunteerId());
             if (volunteer != null) {
                 volunteer.setLikes((volunteer.getLikes() == null ? 0 : volunteer.getLikes()) + 1);
                 userService.updateById(volunteer);
             }
        }
        
        wishService.updateById(wish);
        return Result.success("确认成功！感谢您的反馈，管理员稍后将进行工时与积分核发。");
    }

    /**
     * 单独为完成的心愿补发言赞
     */
    @PutMapping("/like")
    @Operation(summary = "单项心愿点赞", description = "为已完成的志愿者单独追加点赞表扬")
    public Result<String> addLike(@Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId) {
        SysWish wish = wishService.getById(wishId);
        if (wish == null || wish.getVolunteerId() == null) {
            return Result.error(500, "非法的心愿任务");
        }
        if (wish.getIsLiked() != null && wish.getIsLiked() == 1) {
            return Result.success("您已经表扬过他啦！");
        }
        
        wish.setIsLiked(1);
        SysUser volunteer = userService.getById(wish.getVolunteerId());
        if (volunteer != null) {
            volunteer.setLikes((volunteer.getLikes() == null ? 0 : volunteer.getLikes()) + 1);
            userService.updateById(volunteer);
        }
        wishService.updateById(wish);
        return Result.success("红花送达！您的心意已传达给志愿者。");
    }

    /**
     * 获取我相关的心愿
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的心愿记录", description = "居民查询发布记录，志愿者查询认领记录")
    public Result<List<SysWish>> getMyWishes(
            @Parameter(description = "当前用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "用户角色(RESIDENT/VOLUNTEER)", required = true) @RequestParam String role) {
        
        LambdaQueryWrapper<SysWish> wrapper = new LambdaQueryWrapper<>();
        if ("RESIDENT".equals(role)) {
            wrapper.eq(SysWish::getRequesterId, userId);
        } else {
            wrapper.eq(SysWish::getVolunteerId, userId);
        }
        wrapper.orderByDesc(SysWish::getCreateTime);
        
        List<SysWish> list = wishService.list(wrapper);
        for (SysWish wish : list) {
            // 互填信息：居民看谁帮了他，志愿者看谁需要帮
            SysUser req = userService.getById(wish.getRequesterId());
            if (req != null) {
                wish.setRequesterName(req.getRealName());
                wish.setRequesterPhone(req.getPhone());
                wish.setRequesterAvatar(req.getAvatar());
            }
            if (wish.getVolunteerId() != null) {
                SysUser vol = userService.getById(wish.getVolunteerId());
                if (vol != null) {
                    wish.setVolunteerName(vol.getRealName());
                    wish.setVolunteerAvatar(vol.getAvatar());
                }
            }
        }
        return Result.success(list);
    }

    /**
     * 获取全平台最近完成的心愿动态 (展示给居民和志愿者看)
     */
    @GetMapping("/feeds")
    @Operation(summary = "全平台心愿动态流", description = "获取最新完成的 10 条心愿记录")
    public Result<List<SysWish>> getPublicFeeds() {
        LambdaQueryWrapper<SysWish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysWish::getStatus, 3) // 已达成
                .orderByDesc(SysWish::getFinishTime)
                .last("limit 10");
        
        List<SysWish> list = wishService.list(wrapper);
        for (SysWish wish : list) {
            // 数据脱敏处理：保护隐私的同时展示正能量
            SysUser req = userService.getById(wish.getRequesterId());
            if (req != null) {
                if (req.getRealName() != null && !req.getRealName().isEmpty()) {
                    String name = req.getRealName();
                    wish.setRequesterName(name.substring(0, 1) + "**");
                } else {
                    wish.setRequesterName("某居民");
                }
                wish.setRequesterAvatar(req.getAvatar());
            }
            
            if (wish.getVolunteerId() != null) {
                SysUser vol = userService.getById(wish.getVolunteerId());
                if (vol != null) {
                    if (vol.getRealName() != null && !vol.getRealName().isEmpty()) {
                        String name = vol.getRealName();
                        wish.setVolunteerName(name.substring(0, 1) + "志愿者");
                    } else {
                        wish.setVolunteerName("热心志愿者");
                    }
                    wish.setVolunteerAvatar(vol.getAvatar());
                }
            }
        }
        return Result.success(list);
    }

    // ================= 管理员审计端 API =================

    /**
     * 管理员分页审计
     */
    @GetMapping("/admin/page")
    @Operation(summary = "[Admin] 分页审计全局心愿列表")
    public Result<Page<SysWish>> getAdminPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        
        Page<SysWish> page = new Page<>(current, size);
        LambdaQueryWrapper<SysWish> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(SysWish::getStatus, status);
        wrapper.orderByDesc(SysWish::getCreateTime);
        
        wishService.page(page, wrapper);
        
        for (SysWish wish : page.getRecords()) {
            SysUser req = userService.getById(wish.getRequesterId());
            if (req != null) {
                wish.setRequesterName(req.getRealName());
                wish.setRequesterAvatar(req.getAvatar());
            }
            
            if (wish.getVolunteerId() != null) {
                SysUser vol = userService.getById(wish.getVolunteerId());
                if (vol != null) {
                    wish.setVolunteerName(vol.getRealName());
                    wish.setVolunteerAvatar(vol.getAvatar());
                }
            }
        }
        return Result.success(page);
    }

    /**
     * 管理员审核心愿内容
     */
    @PutMapping("/admin/audit")
    @Operation(summary = "[Admin] 审核心愿发布申请", description = "1-审核通过并发布, 4-拒绝发布")
    public Result<String> audit(
            @RequestParam Long wishId,
            @RequestParam Integer status,
            @RequestParam(required = false) String remarks) {
        
        SysWish wish = wishService.getById(wishId);
        wish.setStatus(status);
        wish.setRemarks(remarks);
        wishService.updateById(wish);
        return Result.success("审核操作已落地");
    }

    /**
     * 管理员结算心愿 (核发工时和积分)
     */
    @PostMapping("/admin/settle")
    @Operation(summary = "[Admin] 结算心愿并发放奖励", description = "状态变更为 3-已达成")
    public Result<String> settle(@RequestBody SysWish wishData) {
        SysWish wish = wishService.getById(wishData.getWishId());
        if (wish == null || (wish.getStatus() != 6 && wish.getStatus() != 2)) {
            return Result.error(500, "仅支持结算已确认或进行中的心愿任务");
        }
        
        wish.setStatus(3); // 已达成
        wish.setRewardPoints(wishData.getRewardPoints());
        wish.setRewardHours(wishData.getRewardHours());
        wish.setFinishTime(LocalDateTime.now());
        wishService.updateById(wish);
        
        // 🚨 逻辑注入：对应增加志愿者的积分和工时
        if (wish.getVolunteerId() != null) {
            SysUser user = userService.getById(wish.getVolunteerId());
            if (user != null) {
                user.setCurrentPoints((user.getCurrentPoints() == null ? 0 : user.getCurrentPoints()) + wishData.getRewardPoints());
                user.setTotalPoints((user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + wishData.getRewardPoints());
                
                // 修复：Double 转 BigDecimal 的加法
                BigDecimal hoursToAdd = wishData.getRewardHours() == null ? BigDecimal.ZERO : BigDecimal.valueOf(wishData.getRewardHours());
                user.setTotalHours((user.getTotalHours() == null ? BigDecimal.ZERO : user.getTotalHours()).add(hoursToAdd));
                
                userService.updateById(user);
            }
        }
        
        return Result.success("心愿功德圆满！奖励已落入志愿者账户。");
    }

    /**
     * 删除心愿记录 (主要用于居民删除被拒绝的心愿)
     */
    @DeleteMapping("/{wishId}")
    @Operation(summary = "删除心愿记录", description = "允许删除处于被拒绝状态的微心愿")
    public Result<String> deleteWish(@PathVariable Long wishId) {
        SysWish wish = wishService.getById(wishId);
        if (wish == null) {
            return Result.error(404, "该记录不存在");
        }
        if (wish.getStatus() != 4) {
            return Result.error(403, "只能删除被拒绝状态的心愿记录");
        }
        wishService.removeById(wishId);
        return Result.success("清理成功！");
    }
}
