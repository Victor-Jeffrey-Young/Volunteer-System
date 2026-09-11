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

import java.security.SecureRandom;

/**
 * 兑换记录服务实现类。
 *
 * 并发要点（这是本项目里「做对了」的对照模块）：
 *   - 商品行用 SELECT ... FOR UPDATE 锁住，库存扣减在锁内完成，防超卖；
 *   - 用户积分用「条件 UPDATE」扣减，余额判断与扣减是一条原子语句，防透支；
 *   - 核销用「条件 UPDATE」翻转状态，防一码两用。
 */
@Slf4j
@Service
public class SysExchangeRecordServiceImpl
        extends ServiceImpl<SysExchangeRecordMapper, SysExchangeRecord>
        implements SysExchangeRecordService {

    /** 兑换码字符集：去掉 0/O、1/I 等易混字符，便于线下口头核对 */
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    /** 随机部分长度：32^10 ≈ 1.1e15，配合唯一索引后碰撞概率可忽略 */
    private static final int CODE_RANDOM_LENGTH = 10;
    /** 生成冲突时的重试次数 */
    private static final int CODE_MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String exchange(Long userId, Long goodsId) {
        // 1. 资源查询与锁定（商品行加悲观锁，同一商品的兑换请求排队执行）
        SysGoods goods = goodsService.getByIdForUpdate(goodsId);
        SysUser user = userService.getById(userId);

        if (goods == null) throw new ServiceException(404, "商品不存在");
        if (user == null) throw new ServiceException(404, "用户不存在");

        // 2. 前置校验：这里只为给出友好提示，真正的把关在下面的原子语句里
        if (goods.getStock() <= 0) {
            throw new ServiceException(400, "抱歉，该商品已被兑换完");
        }
        if (user.getCurrentPoints() == null || user.getCurrentPoints() < goods.getPointsRequired()) {
            throw new ServiceException(400, "抱歉，您的可用积分不足");
        }

        // 3. 资产扣减
        //    积分：条件 UPDATE —— 余额判断与扣减原子完成。
        //    早期写法是「读出来减一减再 updateById」，只锁了商品行没锁用户行，
        //    同一用户并发兑换不同商品时两个事务会读到同一份余额，造成积分透支。
        int deducted = userService.deductPointsIfEnough(userId, goods.getPointsRequired());
        if (deducted == 0) {
            throw new ServiceException(400, "抱歉，您的可用积分不足");
        }

        //    库存：商品行已在事务开始时锁住，锁内读改写是安全的
        goods.setStock(goods.getStock() - 1);
        goodsService.updateById(goods);

        // 4. 生成流水与核销码
        SysExchangeRecord record = new SysExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setCostPoints(goods.getPointsRequired());
        record.setStatus(0); // 待核销

        String code = generateUniqueRedeemCode();
        record.setRedeemCode(code);
        this.save(record);

        log.info("用户ID {} 兑换商品成功, 生成码: {}", userId, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyExchange(String code) {
        LambdaQueryWrapper<SysExchangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysExchangeRecord::getRedeemCode, code);
        SysExchangeRecord record = this.getOne(wrapper);

        // 先查一次只为区分「码不存在」和「码已用过」，给出准确提示
        if (record == null) throw new ServiceException(404, "核销码无效");
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new ServiceException(400, "该码已被使用，请勿重复核销");
        }

        // 真正的核销闸门：条件更新，保证同一个码只会被核销一次
        if (baseMapper.verifyIfPending(code) == 0) {
            throw new ServiceException(400, "该码已被使用，请勿重复核销");
        }
        log.info("兑换码 {} 已成功核销", code);
    }

    /**
     * 生成不可预测的核销码。
     *
     * 旧实现是 "GIFT-" + (System.currentTimeMillis() % 10000) + "-" + userId：
     * 每个用户只有一万种可能，且 userId 直接印在码里，可以被枚举伪造后冒领商品。
     * 现在改为 SecureRandom 生成随机串，并查库确认未被占用。
     */
    private String generateUniqueRedeemCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder("GIFT-");
            for (int i = 0; i < CODE_RANDOM_LENGTH; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();

            boolean exists = this.count(new LambdaQueryWrapper<SysExchangeRecord>()
                    .eq(SysExchangeRecord::getRedeemCode, code)) > 0;
            if (!exists) {
                return code;
            }
            log.warn("核销码 {} 已存在，重新生成（第 {} 次）", code, attempt + 1);
        }
        throw new ServiceException(500, "核销码生成失败，请稍后重试");
    }
}
