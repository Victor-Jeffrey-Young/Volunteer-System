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
import com.volunteer.system.utils.RedeemCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 兑换记录服务实现类。
 *
 * 并发要点（这是本项目里「做对了」的对照模块）：
 *   - 商品行用 SELECT ... FOR UPDATE 锁住，库存扣减在锁内完成，防超卖；
 *   - 用户积分用「条件 UPDATE」扣减，余额判断与扣减是一条原子语句，防透支；
 *   - 核销用「条件 UPDATE」翻转状态，防一码两用；
 *   - 核销码不查库，撞唯一索引就换码重试（唯一索引是唯一的仲裁者）。
 */
@Slf4j
@Service
public class SysExchangeRecordServiceImpl
        extends ServiceImpl<SysExchangeRecordMapper, SysExchangeRecord>
        implements SysExchangeRecordService {

    /** 生成冲突时的重试次数 */
    private static final int CODE_MAX_ATTEMPTS = 5;

    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private RedeemCodeGenerator codeGenerator;

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

        // 4. 生成流水与核销码：撞唯一索引就换码重试
        String code = saveRecordWithFreshCode(userId, goods);

        log.info("用户ID {} 兑换商品成功, 生成码: {}", userId, code);
        return code;
    }

    /**
     * 落库兑换流水；核销码撞唯一索引（MySQL 1062）时换一个码重试。
     *
     * 这里不再「先 count 查重再插入」：那一步本身是「先查后改」的竞态，
     * 查一次库也换不来原子性。唯一性交给 uk_redeem_code 唯一索引裁决，
     * 应用侧只负责在冲突时换码。
     *
     * 为什么可以在同一个事务里重试：MySQL 的唯一键冲突只回滚当前语句，
     * 不会让整个事务失效（这点与 PostgreSQL 不同），因此换码后继续插入是安全的。
     *
     * 每次重试都新建实体：既避免复用可能已被写入自增主键的对象，
     * 也让失败那次的脏状态不会带进下一次尝试。
     */
    private String saveRecordWithFreshCode(Long userId, SysGoods goods) {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            SysExchangeRecord record = new SysExchangeRecord();
            record.setUserId(userId);
            record.setGoodsId(goods.getGoodsId());
            record.setCostPoints(goods.getPointsRequired());
            record.setStatus(0); // 待核销
            record.setRedeemCode(codeGenerator.next());

            try {
                this.save(record);
                return record.getRedeemCode();
            } catch (DuplicateKeyException e) {
                log.warn("核销码 {} 已被占用，换码重试（第 {} 次）",
                        record.getRedeemCode(), attempt + 1);
            }
        }
        // 32^10 ≈ 1.1e15 的空间里连撞 5 次属于极端异常，交给全局异常处理返回业务错误
        throw new ServiceException(500, "核销码生成失败，请稍后重试");
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
}
