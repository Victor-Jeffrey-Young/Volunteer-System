package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysExchangeRecordMapper;
import com.volunteer.system.mapper.SysGoodsMapper;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysExchangeRecordService;
import com.volunteer.system.service.SysGoodsService;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.utils.PasswordUtils;
import com.volunteer.system.utils.RedeemCodeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * 核销码撞唯一索引时的端到端回归（真库 + 受控生成器）。
 *
 * 单元测试只能验证「换码重试」的控制流；真正把码钉死的是数据库上的
 * uk_redeem_code 唯一索引。本用例把生成器换成脚本化的桩：
 * 第一次吐出「库里已存在的码」触发 1062，第二次吐出全新的码 ——
 * 于是可以确定性地观察到：重试真的发生了、落库的是新码、资产只扣了一次。
 *
 * 用 @MockBean 会为该测试单独建一个 Spring 上下文（比共享上下文慢一点），
 * 这是为了在不引入「测试专用开关」的前提下替换生产 Bean。
 */
@SpringBootTest
class SysExchangeRecordCodeConflictTest {

    private static final String CONFLICT_CODE = "GIFT-CONFLICT01";
    private static final String FRESH_CODE = "GIFT-FRESH0001";

    @Autowired private SysExchangeRecordService exchangeRecordService;
    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysGoodsMapper goodsMapper;
    @Autowired private SysExchangeRecordMapper exchangeRecordMapper;

    @MockBean private RedeemCodeGenerator codeGenerator;

    private Long userId;
    private Long goodsId;

    @BeforeEach
    void setUp() {
        String tag = "qa_code_" + System.nanoTime();

        SysUser user = new SysUser();
        user.setUsername(tag);
        user.setPassword(PasswordUtils.encode("qa-123456"));
        user.setRealName("核销码冲突测试");
        user.setRole("VOLUNTEER");
        user.setStatus(1);
        user.setCurrentPoints(100);
        user.setTotalPoints(100);
        user.setTotalHours(BigDecimal.ZERO);
        userService.save(user);
        this.userId = user.getUserId();

        SysGoods goods = new SysGoods();
        goods.setName(tag);
        goods.setDescription("核销码冲突测试");
        goods.setPointsRequired(10);
        goods.setStock(1);
        goods.setCategory("QA");
        goodsService.save(goods);
        this.goodsId = goods.getGoodsId();

        // 先占掉一个码，制造真实存在的唯一索引冲突源
        SysExchangeRecord occupying = new SysExchangeRecord();
        occupying.setUserId(userId);
        occupying.setGoodsId(goodsId);
        occupying.setCostPoints(0);
        occupying.setStatus(0);
        occupying.setRedeemCode(CONFLICT_CODE);
        occupying.setCreateTime(LocalDateTime.now());
        exchangeRecordMapper.insert(occupying);
    }

    @AfterEach
    void tearDown() {
        if (goodsId != null) {
            exchangeRecordMapper.delete(new LambdaQueryWrapper<SysExchangeRecord>()
                    .eq(SysExchangeRecord::getGoodsId, goodsId));
            goodsMapper.deleteById(goodsId);
        }
        if (userId != null) {
            userMapper.deleteById(userId);
        }
    }

    @Test
    @DisplayName("生成器先吐已占用的码：重试后兑换成功，落库的是新码，资产只扣一次")
    void exchange_retriesOnRealUniqueKeyConflict() {
        // 第一次给已存在的码（真的撞唯一索引），第二次给全新的码
        when(codeGenerator.next()).thenReturn(CONFLICT_CODE, FRESH_CODE);

        String code = exchangeRecordService.exchange(userId, goodsId);

        assertNotNull(code);
        assertEquals(FRESH_CODE, code, "返回并落库的应当是新码");

        // 新码确实入库，且库里只有一条新码记录
        Long freshCount = exchangeRecordMapper.selectCount(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getRedeemCode, FRESH_CODE));
        assertEquals(1L, freshCount);
        // 撞码那次没有留下脏记录：同一个码仍然只有最初占位的那一条
        Long conflictCount = exchangeRecordMapper.selectCount(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getRedeemCode, CONFLICT_CODE));
        assertEquals(1L, conflictCount, "失败的插入不应留下第二条同码记录");

        // 资产只扣一次
        assertEquals(0, goodsMapper.selectById(goodsId).getStock().intValue(), "库存 1 只应扣一次");
        assertEquals(90, userMapper.selectById(userId).getCurrentPoints().intValue(), "100 分只应扣 10 分");
    }
}
