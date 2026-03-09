package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.common.Result;
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
import org.springframework.transaction.annotation.Transactional;
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

    // ==========================================
    // 🛍️ 志愿者端：商城浏览与购买接口
    // ==========================================

    @GetMapping("/list")
    @Operation(summary = "获取商品大厅列表", description = "仅查询库存大于0的商品，按上架时间倒序排列")
    public Result<List<SysGoods>> listGoods() {
        LambdaQueryWrapper<SysGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SysGoods::getStock, 0).orderByDesc(SysGoods::getCreateTime);
        return Result.success(goodsService.list(wrapper));
    }

    /**
     * 核心高并发接口：积分兑换商品
     * 原理：利用声明式事务与 MySQL 的行级悲观锁 (FOR UPDATE) 确保数据强一致性，杜绝超卖。
     */
    @PostMapping("/exchange")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "商品积分兑换 (支持防超卖锁)", description = "校验积分与库存，成功后扣减可用积分并生成O2O核销码")
    public Result<String> exchange(
            @Parameter(description = "兑换用户的ID", required = true) @RequestParam Long userId,
            @Parameter(description = "被兑换的商品ID", required = true) @RequestParam Long goodsId) {

        log.info("接收到兑换请求 - 用户ID: {}, 商品ID: {}", userId, goodsId);

        // 1. 使用数据库锁查询商品，防止并发超卖 (此方法需在 Mapper 中手写 SELECT ... FOR UPDATE)
        SysGoods goods = goodsService.getByIdForUpdate(goodsId);
        SysUser user = userService.getById(userId);

        if (goods == null) return Result.error(404, "商品不存在");
        if (user == null) return Result.error(404, "用户不存在");

        // 2. 业务校验：仅判断 "可用消费积分"
        if (user.getCurrentPoints() < goods.getPointsRequired()) {
            return Result.error(400, "抱歉，您的可用积分不足");
        }
        if (goods.getStock() <= 0) {
            return Result.error(400, "抱歉，该商品已被兑换完");
        }

        // 3. 资产扣减 (用户积分与商品库存)
        user.setCurrentPoints(user.getCurrentPoints() - goods.getPointsRequired());
        userService.updateById(user);

        goods.setStock(goods.getStock() - 1);
        goodsService.updateById(goods);

        // 4. 生成审计流水与核销码
        SysExchangeRecord record = new SysExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setCostPoints(goods.getPointsRequired());
        record.setStatus(0); // 0-待核销(未领货)

        // 生成类似 GIFT-8392-1 的短验证码
        String code = "GIFT-" + System.currentTimeMillis() % 10000 + "-" + userId;
        record.setRedeemCode(code);
        recordService.save(record);

        log.info("兑换成功 - 生成核销码: {}", code);
        return Result.success("兑换成功！请凭兑换码 [" + code + "] 到服务中心领取。");
    }

    @GetMapping("/my-exchanges")
    @Operation(summary = "获取我的兑换凭证", description = "用于在个人中心展示兑换券及二维码，按时间倒序")
    public Result<List<SysExchangeRecord>> getMyExchanges(
            @Parameter(description = "志愿者用户ID", required = true) @RequestParam Long userId) {

        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getUserId, userId).orderByDesc(SysExchangeRecord::getCreateTime);
        List<SysExchangeRecord> list = recordService.list(wrapper);

        // 填充商品图片及名称供前端渲染
        for (SysExchangeRecord r : list) {
            SysGoods g = goodsService.getById(r.getGoodsId());
            if (g != null) {
                r.setGoodsName(g.getName());
                r.setGoodsImage(g.getImage());
            }
        }
        return Result.success(list);
    }

    // ==========================================
    // ⚙️ 管理员端：商品维护与流水审计接口
    // ==========================================

    @GetMapping("/admin/page")
    @Operation(summary = "[Admin] 分页查询商品库", description = "供管理员进行商品上下架与库存管理")
    @Parameters({
            @Parameter(name = "current", description = "当前页码", example = "1"),
            @Parameter(name = "size", description = "每页展示数量", example = "10"),
            @Parameter(name = "name", description = "商品名称模糊检索")
    })
    public Result<Page<SysGoods>> getGoodsPage(
            @Parameter(hidden = true) @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

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
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goods.setCreateTime(LocalDateTime.now());
        goodsService.save(goods);
        return Result.success("商品上架成功");
    }

    @PutMapping("/admin/update")
    @Operation(summary = "[Admin] 编辑商品/补充库存")
    public Result<String> updateGoods(
            @RequestBody SysGoods goods,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goodsService.updateById(goods);
        return Result.success("商品信息修改成功");
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "[Admin] 物理下架商品")
    public Result<String> deleteGoods(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goodsService.removeById(id);
        return Result.success("商品下架成功");
    }

    // ==========================================
    // 📦 O2O 核销审计模块
    // ==========================================

    @PostMapping("/admin/verify")
    @Operation(summary = "[Admin] 线下核销兑换码", description = "管理员通过扫码或手输，验证兑换码有效性，将状态翻转为已发货")
    public Result<String> verifyExchange(
            @Parameter(description = "系统生成的兑换码(如: GIFT-xxx)", required = true) @RequestParam String code,
            @Parameter(hidden = true) @RequestHeader("Role") String role) {

        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getRedeemCode, code);
        SysExchangeRecord record = recordService.getOne(wrapper);

        // 状态机校验
        if (record == null) return Result.error(404, "核销码无效");
        if (record.getStatus() == 1) return Result.error(400, "该码已被使用，请勿重复核销");

        // 执行核销状态变更
        record.setStatus(1); // 1-已核销(已发货)
        record.setExchangeTime(LocalDateTime.now());
        recordService.updateById(record);

        log.info("核销成功 - 兑换码: {}, 关联记录ID: {}", code, record.getRecordId());
        return Result.success("核销成功！请发放物品。");
    }

    @GetMapping("/admin/record/page")
    @Operation(summary = "[Admin] 分页查询全局兑换流水帐", description = "支持按状态或核销码筛选，用于对账审计")
    @Parameters({
            @Parameter(name = "code", description = "精准搜索指定核销码"),
            @Parameter(name = "status", description = "0-待核销, 1-已领取")
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