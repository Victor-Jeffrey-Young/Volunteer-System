package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
            @Parameter(description = "兑换用户的ID", required = true) @RequestParam Long userId,
            @Parameter(description = "被兑换的商品ID", required = true) @RequestParam Long goodsId) {

        log.info("接收到兑换请求 - 用户ID: {}, 商品ID: {}", userId, goodsId);

        // 业务下沉到 Service 层，自动处理事务和异常
        String code = recordService.exchange(userId, goodsId);

        return Result.success("兑换成功！请凭兑换码 [" + code + "] 到服务中心领取。");
    }

    // ==========================================
    // ⚙️ 管理员端：商品维护与流水审计接口
    // ==========================================

    @GetMapping("/admin/page")
    @Operation(summary = "[Admin] 分页查询商品库", description = "供管理员进行商品上下架与库存管理")
    public Result<Page<SysGoods>> getGoodsPage(
            @Parameter(hidden = true) @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        if (!"ADMIN".equals(role)) throw new ServiceException(403, "权限不足");

        Page<SysGoods> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysGoods::getName, name)
                .orderByDesc(SysGoods::getCreateTime);

        goodsService.page(pageInfo, wrapper);
        return Result.success(pageInfo);
    }

    @PostMapping("/admin/add")
    @Operation(summary = "[Admin] 上架新商品")
    public Result<String> addGoods(
            @RequestBody SysGoods goods,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) throw new ServiceException(403, "权限不足");

        goods.setCreateTime(LocalDateTime.now());
        goodsService.save(goods);
        return Result.success("商品上架成功");
    }

    @PutMapping("/admin/update")
    @Operation(summary = "[Admin] 编辑商品/补充库存")
    public Result<String> updateGoods(
            @RequestBody SysGoods goods,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) throw new ServiceException(403, "权限不足");

        goodsService.updateById(goods);
        return Result.success("商品信息修改成功");
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "[Admin] 物理下架商品")
    public Result<String> deleteGoods(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) throw new ServiceException(403, "权限不足");

        goodsService.removeById(id);
        return Result.success("商品下架成功");
    }

    // ==========================================
    // 📦 O2O 核销审计模块
    // ==========================================

    @PostMapping("/admin/verify")
    @Operation(summary = "[Admin] 线下核销兑换码", description = "管理员验证兑换码有效性并核销")
    public Result<String> verifyExchange(
            @Parameter(description = "兑换码(GIFT-xxxx)", required = true) @RequestParam String code,
            @Parameter(description = "操作员角色", hidden = true) @RequestHeader("Role") String role) {

        // 业务下沉到 Service
        recordService.verifyExchange(code, role);

        return Result.success("核销成功！请发放物品。");
    }

    @GetMapping("/admin/record/page")
    @Operation(summary = "[Admin] 分页查询全局兑换流水", description = "支持按核销码精准查询或按状态筛选")
    @Parameters({
            @Parameter(name = "current", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "code", description = "兑换码精确搜索"),
            @Parameter(name = "status", description = "状态：0-待核销, 1-已领取")
    })
    public Result<Page<SysExchangeRecord>> getRecordPage(
            @Parameter(hidden = true) @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status) {

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        Page<SysExchangeRecord> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();

        // 动态条件构建
        wrapper.eq(StringUtils.hasText(code), SysExchangeRecord::getRedeemCode, code);
        if (status != null) {
            wrapper.eq(SysExchangeRecord::getStatus, status);
        }
        wrapper.orderByDesc(SysExchangeRecord::getCreateTime);

        recordService.page(pageInfo, wrapper);

        // 连表查询组装人名与商品名 (防孤儿记录兜底)
        for (SysExchangeRecord record : pageInfo.getRecords()) {
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
        return Result.success(pageInfo);
    }
}