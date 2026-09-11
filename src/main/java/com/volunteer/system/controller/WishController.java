package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
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

import java.util.List;

/**
 * 邻里微心愿控制器。
 *
 * 职责边界：本类只做「取参数 → 调 Service → 包装 Result」，
 * 状态流转、归属校验、奖励发放全部在 SysWishService 内完成。
 * 身份一律取自 JWT（@RequestAttribute），不接受任何客户端传入的 userId / requesterId。
 */
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
    @Operation(summary = "居民提交心愿", description = "发布者身份取自 JWT，忽略请求体中的 requesterId")
    public Result<String> apply(
            @RequestBody SysWish wish,
            @RequestAttribute("userId") Long userId) {
        wishService.applyWish(wish, userId);
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
            @RequestAttribute("userId") Long userId) {
        wishService.claimWish(wishId, userId);
        return Result.success("成功认领！请尽快与居民取得联系并提供服务。");
    }

    /**
     * 志愿者标记已完成
     */
    @PutMapping("/finish")
    @Operation(summary = "志愿者标记服务完成", description = "只能操作自己认领的心愿")
    public Result<String> finish(
            @Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId,
            @RequestAttribute("userId") Long userId) {
        wishService.finishWish(wishId, userId);
        return Result.success("服务记录已提交，请等待居民确认。");
    }

    /**
     * 居民点击确认完成
     */
    @PutMapping("/confirm")
    @Operation(summary = "居民确认服务完成", description = "只能确认自己发布的心愿，可附带点赞与评价")
    public Result<String> confirm(
            @Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId,
            @Parameter(description = "是否点赞") @RequestParam(required = false) Boolean liked,
            @Parameter(description = "反馈评价信息") @RequestParam(required = false) String rateMsg,
            @RequestAttribute("userId") Long userId) {
        wishService.confirmWish(wishId, userId, liked, rateMsg);
        return Result.success("确认成功！感谢您的反馈，管理员稍后将进行工时与积分核发。");
    }

    /**
     * 单独为完成的心愿补发言赞
     */
    @PutMapping("/like")
    @Operation(summary = "单项心愿点赞", description = "为已完成的心愿追加点赞表扬，重复调用不会重复计数")
    public Result<String> addLike(
            @Parameter(description = "心愿记录ID", required = true) @RequestParam Long wishId,
            @RequestAttribute("userId") Long userId) {
        wishService.likeWish(wishId, userId);
        return Result.success("红花送达！您的心意已传达给志愿者。");
    }

    /**
     * 获取我相关的心愿
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的心愿记录", description = "居民查询发布记录，志愿者查询认领记录")
    public Result<List<SysWish>> getMyWishes(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("role") String role) {
        
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
                    wish.setVolunteerPhone(vol.getPhone());
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
    @RequiresAdmin
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
                    wish.setVolunteerPhone(vol.getPhone());
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
    @RequiresAdmin
    @Operation(summary = "[Admin] 审核心愿发布申请", description = "1-审核通过并发布, 4-拒绝发布")
    public Result<String> audit(
            @RequestParam Long wishId,
            @RequestParam Integer status,
            @RequestParam(required = false) String remarks) {

        if (status == null || (status != 1 && status != 4)) {
            throw new ServiceException(400, "审核结果只能是 1-通过 或 4-驳回");
        }
        SysWish wish = wishService.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        wish.setStatus(status);
        wish.setRemarks(remarks);
        wishService.updateById(wish);
        return Result.success("审核操作已落地");
    }

    /**
     * 管理员结算心愿 (核发工时和积分)
     */
    @PostMapping("/admin/settle")
    @RequiresAdmin
    @Operation(summary = "[Admin] 结算心愿并发放奖励", description = "状态变更为 3-已达成，积分工时原子累加")
    public Result<String> settle(@RequestBody SysWish wishData) {
        wishService.settleWish(wishData.getWishId(),
                wishData.getRewardPoints(), wishData.getRewardHours());
        return Result.success("心愿功德圆满！奖励已落入志愿者账户。");
    }

    /**
     * 删除心愿记录 (主要用于居民删除被拒绝的心愿)
     */
    @DeleteMapping("/{wishId}")
    @Operation(summary = "删除心愿记录", description = "仅发布人本人可删除处于被拒绝状态的心愿")
    public Result<String> deleteWish(
            @PathVariable Long wishId,
            @RequestAttribute("userId") Long userId) {
        wishService.deleteRejectedWish(wishId, userId);
        return Result.success("清理成功！");
    }
}
