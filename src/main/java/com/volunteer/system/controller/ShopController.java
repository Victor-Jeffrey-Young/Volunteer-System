package com.volunteer.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.common.Result;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysExchangeRecordService;
import com.volunteer.system.service.SysGoodsService;
import com.volunteer.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private SysExchangeRecordService recordService;

    @GetMapping("/list")
    public Result<List<SysGoods>> listGoods() {
        return Result.success(goodsService.list());
    }

    @PostMapping("/exchange")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> exchange(@RequestParam Long userId, @RequestParam Long goodsId) {
        // 使用数据库锁，防止并发超卖 (论文亮点)
        SysGoods goods = goodsService.getByIdForUpdate(goodsId);
        SysUser user = userService.getById(userId);

        if (goods == null) return Result.error(404, "商品不存在");
        if (user == null) return Result.error(404, "用户不存在");

        if (user.getCurrentPoints() < goods.getPointsRequired())
            return Result.error(400, "抱歉，您的可用积分不足");
        if (goods.getStock() <= 0) return Result.error(400, "抱歉，该商品已被兑换完");

        user.setCurrentPoints(user.getCurrentPoints() - goods.getPointsRequired());
        userService.updateById(user);

        goods.setStock(goods.getStock() - 1);
        goodsService.updateById(goods);

        SysExchangeRecord record = new SysExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setCostPoints(goods.getPointsRequired());
        record.setStatus(0); // 0-待核销

        // 🚨 生成 8 位随机核销码 (大写字母+数字)
        // 如果没有引入 Hutool，可以用: UUID.randomUUID().toString().substring(0,8).toUpperCase();
        String code = "GIFT-" + System.currentTimeMillis() % 10000 + "-" + userId;
        record.setRedeemCode(code);

        recordService.save(record);

        return Result.success("兑换成功！请凭兑换码 [" + code + "] 到服务中心领取。");
    }


    // ==========================================
    //  管理员端商品管理接口
    // ==========================================
    /**
     * 1. 分页查询商品列表 (管理员用)
     */
    @GetMapping("/admin/page")
    public Result<Page<SysGoods>> getGoodsPage(
            @RequestHeader("Role") String role,
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

    /**
     * 2. 新增商品上架
     */
    @PostMapping("/admin/add")
    public Result<String> addGoods(@RequestBody SysGoods goods, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goods.setCreateTime(LocalDateTime.now());
        goodsService.save(goods);
        return Result.success("商品上架成功");
    }

    /**
     * 3. 修改商品信息 (如补充库存、改价)
     */
    @PutMapping("/admin/update")
    public Result<String> updateGoods(@RequestBody SysGoods goods, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goodsService.updateById(goods);
        return Result.success("商品信息修改成功");
    }

    /**
     * 4. 删除商品下架
     */
    @DeleteMapping("/admin/{id}")
    public Result<String> deleteGoods(@PathVariable Long id, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        goodsService.removeById(id);
        return Result.success("商品下架成功");
    }

    // 5. 管理员核销接口 (线下发货)
    @PostMapping("/admin/verify")
    public Result<String> verifyExchange(@RequestParam String code, @RequestHeader("Role") String role) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getRedeemCode, code);
        SysExchangeRecord record = recordService.getOne(wrapper);

        if (record == null) return Result.error(404, "核销码无效");
        if (record.getStatus() == 1) return Result.error(400, "该码已被使用，请勿重复核销");

        // 更新状态
        record.setStatus(1); // 1-已核销/已领取
        record.setExchangeTime(LocalDateTime.now());
        recordService.updateById(record);

        return Result.success("核销成功！请发放物品。");
    }

    // 6. 用户查询自己的兑换记录
    @GetMapping("/my-exchanges")
    public Result<List<SysExchangeRecord>> getMyExchanges(@RequestParam Long userId) {
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getUserId, userId).orderByDesc(SysExchangeRecord::getCreateTime);
        List<SysExchangeRecord> list = recordService.list(wrapper);

        // 填充商品信息
        for (SysExchangeRecord r : list) {
            SysGoods g = goodsService.getById(r.getGoodsId());
            if (g != null) {
                r.setGoodsName(g.getName());
                r.setGoodsImage(g.getImage());
            }
        }
        return Result.success(list);
    }

    // 7. 🚨 新增：管理员分页查询所有兑换记录
    @GetMapping("/admin/record/page")
    public Result<Page<SysExchangeRecord>> getRecordPage(
            @RequestHeader("Role") String role,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String code, // 按核销码搜索
            @RequestParam(required = false) Integer status // 按状态筛选
    ) {
        if (!"ADMIN".equals(role)) return Result.error(403, "权限不足");

        Page<SysExchangeRecord> pageInfo = new Page<>(current, size);
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();

        // 动态条件
        wrapper.eq(StringUtils.hasText(code), SysExchangeRecord::getRedeemCode, code);
        if (status != null) {
            wrapper.eq(SysExchangeRecord::getStatus, status);
        }

        wrapper.orderByDesc(SysExchangeRecord::getCreateTime);

        recordService.page(pageInfo, wrapper);

        // 填充关联信息 (用户名的商品名)
        for (SysExchangeRecord record : pageInfo.getRecords()) {
            // 查用户
            SysUser user = userService.getById(record.getUserId());
            if (user != null) record.setUserName(user.getRealName());
            else record.setUserName("未知用户");

            // 查商品
            SysGoods goods = goodsService.getById(record.getGoodsId());
            if (goods != null) {
                record.setGoodsName(goods.getName());
                record.setGoodsImage(goods.getImage());
            } else {
                record.setGoodsName("商品已下架");
            }
        }

        return Result.success(pageInfo);
    }
}