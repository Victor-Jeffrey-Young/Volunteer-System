package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.RequiresAdmin;
import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysExchangeRecordService;
import com.volunteer.system.service.SysGoodsService;
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
import java.util.List;

/**
 * 积分商城与 O2O 核销控制器
 * 负责商品管理、高并发防超卖兑换、核销码生成及兑换流水审计。
 */
@Slf4j
@RestController
@RequestMapping("/api/shop")
@Tag(name = "05. 积分商城与激励模块", description = "双轨制积分消费、商品兑换及线下扫码核销闭环")
public class ShopController {

    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private SysExchangeRecordService recordService;

    /**
     * 核心高并发接口：积分兑换商品
     */
    @PostMapping("/exchange")
    @Operation(summary = "商品积分兑换 (支持防超卖锁)", description = "校验积分与库存，成功后扣减可用积分并生成O2O核销码")
    public Result<String> exchange(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "被兑换的商品ID", required = true) @RequestParam Long goodsId) {

        log.info("接收到兑换请求 - 用户ID: {}, 商品ID: {}", userId, goodsId);
        String code = recordService.exchange(userId, goodsId);
        return Result.success("兑换成功！请凭兑换码 [" + code + "] 到服务中心领取。");
    }

    // ==========================================
    // 管理员端：商品维护与流水审计接口
    // ==========================================

    @GetMapping("/admin/page")
    @RequiresAdmin
    @Operation(summary = "[Admin] 分页查询商品库", description = "供管理员进行商品上下架与库存管理")
    public Result<Page<SysGoods>> getGoodsPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        Page<SysGoods> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysGoods::getName, name)
                .orderByDesc(SysGoods::getCreateTime);

        goodsService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    @PostMapping("/admin/add")
    @RequiresAdmin
    @Operation(summary = "[Admin] 上架新商品")
    public Result<String> addGoods(@RequestBody SysGoods goods) {

        if (goods.getStock() == null || goods.getStock() <= 0) {
            throw new ServiceException(400, "上架商品库存必须大于 0");
        }
        if (goods.getPointsRequired() == null || goods.getPointsRequired() < 0) {
            throw new ServiceException(400, "商品积分必须大于或等于 0");
        }

        goods.setCreateTime(LocalDateTime.now());
        goodsService.save(goods);
        return Result.success("商品上架成功");
    }

    @PutMapping("/admin/update")
    @RequiresAdmin
    @Operation(summary = "[Admin] 编辑商品/补充库存")
    public Result<String> updateGoods(@RequestBody SysGoods goods) {

        if (goods.getStock() != null && goods.getStock() < 0) {
            throw new ServiceException(400, "商品库存不能小于 0");
        }
        if (goods.getPointsRequired() != null && goods.getPointsRequired() < 0) {
            throw new ServiceException(400, "商品积分必须大于或等于 0");
        }

        goodsService.updateById(goods);
        return Result.success("商品信息修改成功");
    }

    @DeleteMapping("/admin/{id}")
    @RequiresAdmin
    @Operation(summary = "[Admin] 物理下架商品")
    public Result<String> deleteGoods(
            @Parameter(description = "商品ID") @PathVariable Long id) {

        goodsService.removeById(id);
        return Result.success("商品下架成功");
    }

    // ==========================================
    // O2O 核销审计模块
    // ==========================================

    @PostMapping("/admin/verify")
    @RequiresAdmin
    @Operation(summary = "[Admin] 线下核销兑换码", description = "管理员验证兑换码有效性并核销")
    public Result<String> verifyExchange(
            @Parameter(description = "兑换码(GIFT-xxxx)", required = true) @RequestParam String code) {

        recordService.verifyExchange(code);
        return Result.success("核销成功！请发放物品。");
    }

    @GetMapping("/admin/record/page")
    @RequiresAdmin
    @Operation(summary = "[Admin] 分页查询全局兑换流水", description = "支持按核销码精准查询或按状态筛选")
    @Parameters({
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "code", description = "兑换码精确搜索"),
            @Parameter(name = "status", description = "状态：0-待核销, 1-已领取")
    })
    public Result<Page<SysExchangeRecord>> getRecordPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status) {

        Page<SysExchangeRecord> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();

        // 动态条件构建
        wrapper.eq(StringUtils.hasText(code), SysExchangeRecord::getRedeemCode, code);
        if (status != null) {
            wrapper.eq(SysExchangeRecord::getStatus, status);
        }
        wrapper.orderByDesc(SysExchangeRecord::getCreateTime);

        recordService.page(pageInfo, wrapper);

        enrichRecords(pageInfo.getRecords());
        return Result.success(pageInfo);
    }

    // ==========================================
    // 志愿者端：商品大厅与个人兑换记录
    // (不做 ADMIN 校验；登录用户即可浏览，身份一律取自 JWT)
    // ==========================================

    /**
     * 商品大厅：任何登录用户浏览可兑换商品
     */
    @GetMapping("/page")
    @Operation(summary = "积分商品大厅", description = "志愿者浏览可兑换商品（无需管理员权限）")
    public Result<Page<SysGoods>> getGoodsPageForUser(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "48") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {

        Page<SysGoods> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysGoods::getName, name)
                .eq(StringUtils.hasText(category), SysGoods::getCategory, category)
                .orderByDesc(SysGoods::getCreateTime);

        goodsService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    /**
     * 我的兑换记录：仅返回当前登录用户（JWT 身份）的兑换流水
     */
    @GetMapping("/my-record")
    @Operation(summary = "我的兑换记录", description = "仅返回当前登录用户自己的兑换流水，identity 取自 JWT")
    public Result<Page<SysExchangeRecord>> getMyRecordPage(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "100") Integer size) {

        Page<SysExchangeRecord> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getUserId, userId)
                .orderByDesc(SysExchangeRecord::getCreateTime);

        recordService.page(pageInfo, wrapper);
        enrichRecords(pageInfo.getRecords());
        return Result.success(pageInfo);
    }

    /**
     * 兑换流水连表组装：补全用户名与商品名（防孤儿记录兜底）
     */
    private void enrichRecords(List<SysExchangeRecord> records) {
        for (SysExchangeRecord record : records) {
            SysUser user = userService.getById(record.getUserId());
            record.setUserName(user != null ? user.getRealName() : "未知用户");

            SysGoods goods = goodsService.getById(record.getGoodsId());
            if (goods != null) {
                record.setGoodsName(goods.getName());
                record.setGoodsImage(goods.getImage());
            } else {
                record.setGoodsName("【商品已下架】");
            }
        }
    }
}