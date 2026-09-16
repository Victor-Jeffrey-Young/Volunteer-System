package com.volunteer.system.concurrency;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 真实锁冲突 → Spring 异常翻译的端到端验证。
 *
 * 单元测试只能证明「给 handler 一个 X 异常，它会返回 409」；本用例回答的是更关键的一问：
 * **MySQL 的锁等待超时真的会被翻译成 PessimisticLockingFailureException 吗？**
 * 做法：把连接池的 innodb_lock_wait_timeout 压到 1 秒
 * （`connection-init-sql`，只影响本测试的上下文），另开一条 JDBC 连接把商品行锁住，
 * 再从应用侧发起一次需要锁同一行的兑换 —— 1 秒后数据库报 1205，
 * 断言应用侧收到的正是该异常家族。
 *
 * 这样 S4 的 409 分支就有了完整证据链：DB 错误码 → Spring 异常 → handler → 409。
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=1"
})
class LockTimeoutTest {

    @Autowired private DataSource dataSource;
    @Autowired private SysExchangeRecordService exchangeRecordService;
    @Autowired private SysUserService userService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private SysUserMapper userMapper;
    @Autowired private SysGoodsMapper goodsMapper;
    @Autowired private SysExchangeRecordMapper exchangeRecordMapper;

    private Long userId;
    private Long goodsId;
    private Connection lockHolder;

    @BeforeEach
    void setUp() {
        String tag = "qa_lock_" + System.nanoTime();

        SysUser user = new SysUser();
        user.setUsername(tag);
        user.setPassword(PasswordUtils.encode("qa-123456"));
        user.setRealName("锁等待测试");
        user.setRole("VOLUNTEER");
        user.setStatus(1);
        user.setCurrentPoints(100);
        user.setTotalPoints(100);
        user.setTotalHours(BigDecimal.ZERO);
        userService.save(user);
        this.userId = user.getUserId();

        SysGoods goods = new SysGoods();
        goods.setName(tag);
        goods.setDescription("锁等待测试");
        goods.setPointsRequired(10);
        goods.setStock(1);
        goods.setCategory("QA");
        goodsService.save(goods);
        this.goodsId = goods.getGoodsId();
    }

    @AfterEach
    void tearDown() throws Exception {
        releaseLock();
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
    @DisplayName("商品行被别的连接锁住：1 秒后抛 PessimisticLockingFailureException，且不产生脏数据")
    void exchangeIsRejectedAsLockConflictWhenRowIsLocked() throws Exception {
        lockGoodsRow();

        PessimisticLockingFailureException ex = assertThrows(PessimisticLockingFailureException.class,
                () -> exchangeRecordService.exchange(userId, goodsId),
                "锁等待超时应被翻译成 PessimisticLockingFailureException（handler 据此返回 409）");

        // 事务已回滚：库存与积分都没动，也没有留下流水
        assertEquals(1, goodsMapper.selectById(goodsId).getStock().intValue(), "失败的兑换不应扣库存");
        assertEquals(100, userMapper.selectById(userId).getCurrentPoints().intValue(), "失败的兑换不应扣积分");
        assertEquals(0L, exchangeRecordMapper.selectCount(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getGoodsId, goodsId)), "不应留下兑换流水");
        System.out.println("锁冲突异常类型: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("对照：锁释放后同一笔兑换可以正常完成")
    void exchangeSucceedsAfterLockReleased() throws Exception {
        lockGoodsRow();
        releaseLock();

        String code = exchangeRecordService.exchange(userId, goodsId);

        assertEquals(0, goodsMapper.selectById(goodsId).getStock().intValue(), "释放锁后应正常扣减");
        assertEquals(90, userMapper.selectById(userId).getCurrentPoints().intValue());
        assertEquals(1L, exchangeRecordMapper.selectCount(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getRedeemCode, code)));
    }

    /** 另开一条连接，开启事务并锁住商品行（不提交，直到 releaseLock） */
    private void lockGoodsRow() throws Exception {
        lockHolder = dataSource.getConnection();
        lockHolder.setAutoCommit(false);
        try (PreparedStatement ps = lockHolder.prepareStatement(
                "SELECT goods_id FROM sys_goods WHERE goods_id = ? FOR UPDATE")) {
            ps.setLong(1, goodsId);
            ps.executeQuery();
        }
    }

    private void releaseLock() throws Exception {
        if (lockHolder != null) {
            lockHolder.rollback();
            lockHolder.close();
            lockHolder = null;
        }
    }
}
