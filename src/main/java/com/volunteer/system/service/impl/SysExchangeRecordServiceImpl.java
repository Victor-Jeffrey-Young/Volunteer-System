package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysExchangeRecordMapper;
import com.volunteer.system.service.SysExchangeRecordService;
import com.volunteer.system.service.SysGoodsService;
import com.volunteer.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 兑换记录服务实现类
 */
@Slf4j
@Service
public class SysExchangeRecordServiceImpl
        extends ServiceImpl<SysExchangeRecordMapper, SysExchangeRecord>
        implements SysExchangeRecordService {

    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String exchange(Long userId, Long goodsId) {
        // 1. 资源查询与锁定
        SysGoods goods = goodsService.getByIdForUpdate(goodsId);
        SysUser user = userService.getById(userId);

        if (goods == null) throw new ServiceException(404, "商品不存在");
        if (user == null) throw new ServiceException(404, "用户不存在");

        // 2. 核心业务校验
        if (user.getCurrentPoints() < goods.getPointsRequired()) {
            throw new ServiceException(400, "抱歉，您的可用积分不足");
        }
        if (goods.getStock() <= 0) {
            throw new ServiceException(400, "抱歉，该商品已被兑换完");
        }

        // 3. 资产扣减
        user.setCurrentPoints(user.getCurrentPoints() - goods.getPointsRequired());
        userService.updateById(user);

        goods.setStock(goods.getStock() - 1);
        goodsService.updateById(goods);

        // 4. 生成流水与核销码
        SysExchangeRecord record = new SysExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setCostPoints(goods.getPointsRequired());
        record.setStatus(0); // 待核销

        String code = "GIFT-" + (System.currentTimeMillis() % 10000) + "-" + userId;
        record.setRedeemCode(code);
        this.save(record);

        log.info("用户ID {} 兑换商品成功, 生成码: {}", userId, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyExchange(String code, String role) {
        if (!"ADMIN".equals(role)) {
            throw new ServiceException(403, "权限不足，无法执行核销操作");
        }

        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getRedeemCode, code);
        SysExchangeRecord record = this.getOne(wrapper);

        // 状态机校验
        if (record == null) throw new ServiceException(404, "核销码无效");
        if (record.getStatus() == 1) throw new ServiceException(400, "该码已被使用，请勿重复核销");

        // 执行核销
        record.setStatus(1); // 已发货
        record.setExchangeTime(LocalDateTime.now());
        this.updateById(record);
        log.info("兑换码 {} 已成功核销", code);
    }
}
