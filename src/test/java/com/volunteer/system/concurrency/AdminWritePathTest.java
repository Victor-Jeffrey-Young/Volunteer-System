package com.volunteer.system.concurrency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.controller.ActivityController;
import com.volunteer.system.controller.ShopController;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysRegistration;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysActivityMapper;
import com.volunteer.system.mapper.SysExchangeRecordMapper;
import com.volunteer.system.mapper.SysGoodsMapper;
import com.volunteer.system.mapper.SysRegistrationMapper;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysExchangeRecordService;
import com.volunteer.system.service.SysGoodsService;
import com.volunteer.system.service.SysRegistrationService;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.utils.PasswordUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 管理端写路径的并发回归测试（真库）。
 *
 * 用户的报名/兑换链路有行锁和条件更新保护，管理端的「编辑」如果整体写回实体，
 * 就会把这些保护成果覆盖掉：管理员打开弹窗时拿到的是旧快照，点保存时写回的是旧值，
 * 于是并发期间发生的报名、兑换全部丢失（实测：库存 1 的商品被兑换后，
 * 管理员提交旧表单把库存写回 1，同一商品成交 2 单）。
 *
 * 这些用例直接调用 Controller 方法（不经过 AdminInterceptor 鉴权，
 * 鉴权另有 AdminAuthorizationTest 覆盖），验证的是写路径本身。
 */
@SpringBootTest
class AdminWritePathTest {

    @Autowired private ActivityController activityController;
    @Autowired private ShopController shopController;
    @Autowired private SysActivityService activityService;
    @Autowired private SysGoodsService goodsService;
    @Autowired private SysRegistrationService registrationService;
    @Autowired private SysExchangeRecordService exchangeRecordService;
    @Autowired private SysUserService userService;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysGoodsMapper goodsMapper;
    @Autowired private SysExchangeRecordMapper exchangeRecordMapper;
    @Autowired private SysRegistrationMapper registrationMapper;
    @Autowired private SysUserMapper userMapper;

    private static final int THREADS = 5;

    private Long userId;
    private Long activityId;

    @BeforeEach
    void setUp() {
        String tag = "qa_admin_" + System.nanoTime();

        SysUser user = new SysUser();
        user.setUsername(tag);
        user.setPassword(PasswordUtils.encode("qa-123456"));
        user.setRealName("管理端写路径测试");
        user.setRole("VOLUNTEER");
        user.setStatus(1);
        user.setCurrentPoints(100);
        user.setTotalPoints(100);
        user.setTotalHours(BigDecimal.ZERO);
        userService.save(user);
        this.userId = user.getUserId();

        SysActivity activity = new SysActivity();
        activity.setTitle(tag);
        activity.setContent("管理端写路径测试");
        activity.setType("QA");
        activity.setCapacity(5);
        activity.setCurrentNum(0);
        activity.setStatus(0);
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        activity.setRewardHours(new BigDecimal("2.00"));
        activityService.save(activity);
        this.activityId = activity.getActivityId();
    }

    @AfterEach
    void tearDown() {
        if (activityId != null) {
            registrationMapper.delete(new LambdaQueryWrapper<SysRegistration>()
                    .eq(SysRegistration::getActivityId, activityId));
            activityMapper.deleteById(activityId);
        }
        if (userId != null) {
            userMapper.deleteById(userId);
        }
    }

    @Test
    @DisplayName("管理端编辑活动：旧表单里的 currentNum 不得覆盖真实报名数")
    void adminEditActivity_doesNotOverwriteCurrentNum() {
        // 报名一人，计数变成 1
        registrationService.applyActivity(userId, activityId);
        assertEquals(1, currentNum());

        // 管理员提交的是「打开弹窗那一刻」的旧快照：currentNum=0
        SysActivity staleForm = new SysActivity();
        staleForm.setActivityId(activityId);
        staleForm.setTitle("改名后的活动");
        staleForm.setContent("新的活动说明");
        staleForm.setCapacity(8);
        staleForm.setCurrentNum(0);
        staleForm.setStatus(1);

        activityController.updateActivity(staleForm);

        SysActivity after = activityMapper.selectById(activityId);
        assertEquals(1, after.getCurrentNum().intValue(), "派生计数必须保持真实值，不能被旧表单覆盖");
        assertEquals("改名后的活动", after.getTitle(), "白名单内的字段应正常更新");
        assertEquals(8, after.getCapacity().intValue());
        assertEquals(1, after.getStatus().intValue());
    }

    // ---------------------------------------------------------------- 工具方法

    private int currentNum() {
        return activityMapper.selectById(activityId).getCurrentNum();
    }

    // ================================================================ 积分商城库存

    @Test
    @DisplayName("管理端编辑商品：旧表单里的 stock 不得覆盖并发扣减后的库存")
    void adminEditGoods_doesNotOverwriteStock() {
        Long goodsId = createGoodsWithStock(1);
        try {
            // 用户兑换掉唯一一件（库存 0，1 条流水）
            String code = exchangeRecordService.exchange(userId, goodsId);
            assertNotNull(code);
            assertEquals(0, stockOf(goodsId));

            // 管理员提交「打开弹窗时」的旧快照：stock=1
            SysGoods staleForm = new SysGoods();
            staleForm.setGoodsId(goodsId);
            staleForm.setName("改名后的商品");
            staleForm.setPointsRequired(20);
            staleForm.setStock(1);

            shopController.updateGoods(staleForm);

            SysGoods after = goodsMapper.selectById(goodsId);
            assertEquals(0, after.getStock().intValue(), "库存必须保持并发扣减后的真实值");
            assertEquals("改名后的商品", after.getName(), "白名单内的字段应正常更新");
            assertEquals(20, after.getPointsRequired().intValue());
        } finally {
            cleanGoods(goodsId);
        }
    }

    @Test
    @DisplayName("并发兑换库存 1 的商品：只允许成交一单（原有防线保持有效）")
    void concurrentExchange_onlyOneSucceeds() throws Exception {
        Long goodsId = createGoodsWithStock(1);
        try {
            int success = runConcurrently(() -> exchangeRecordService.exchange(userId, goodsId));

            assertEquals(1, success, "5 个并发兑换请求应当只有 1 个成功");
            assertEquals(0, stockOf(goodsId));
            assertEquals(1L, exchangeCount(goodsId));
        } finally {
            cleanGoods(goodsId);
        }
    }

    @Test
    @DisplayName("并发补货：增量累加不丢失，且不会把库存补成负数")
    void concurrentRestock_accumulatesExactly() throws Exception {
        Long goodsId = createGoodsWithStock(0);
        try {
            // 5 个并发补货请求，各 +2
            int success = runConcurrently(() -> {
                if (goodsService.adjustStock(goodsId, 2) == 0) {
                    throw new IllegalStateException("补货未命中");
                }
            });
            assertEquals(5, success);
            assertEquals(10, stockOf(goodsId), "5 次 +2 必须精确累加");

            // 盘亏超过现有库存时必须被守卫拦下，库存不会变成负数
            assertEquals(0, goodsService.adjustStock(goodsId, -11));
            assertEquals(10, stockOf(goodsId));
        } finally {
            cleanGoods(goodsId);
        }
    }

    private Long createGoodsWithStock(int stock) {
        SysGoods goods = new SysGoods();
        goods.setName("qa_goods_" + System.nanoTime());
        goods.setDescription("管理端写路径测试");
        goods.setPointsRequired(10);
        goods.setStock(stock);
        goods.setCategory("QA");
        goodsService.save(goods);
        return goods.getGoodsId();
    }

    private void cleanGoods(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        exchangeRecordMapper.delete(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getGoodsId, goodsId));
        goodsMapper.deleteById(goodsId);
    }

    private int stockOf(Long goodsId) {
        return goodsMapper.selectById(goodsId).getStock();
    }

    private long exchangeCount(Long goodsId) {
        return exchangeRecordMapper.selectCount(new LambdaQueryWrapper<SysExchangeRecord>()
                .eq(SysExchangeRecord::getGoodsId, goodsId));
    }

    /** 与 RegistrationConcurrencyTest 相同的并发工具：N 个线程尽量同时起跑，返回成功次数 */
    private int runConcurrently(Runnable action) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(THREADS);
        AtomicInteger success = new AtomicInteger();
        List<Throwable> unexpected = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    action.run();
                    success.incrementAndGet();
                } catch (Exception e) {
                    if (!(e instanceof com.volunteer.system.common.ServiceException)) {
                        synchronized (unexpected) {
                            unexpected.add(e);
                        }
                    }
                } finally {
                    finishGate.countDown();
                }
            });
        }

        startGate.countDown();
        finishGate.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        if (!unexpected.isEmpty()) {
            throw new AssertionError("出现非业务预期的异常：" + unexpected.get(0), unexpected.get(0));
        }
        return success.get();
    }
}
