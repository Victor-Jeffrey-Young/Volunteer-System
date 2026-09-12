package com.volunteer.system.concurrency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysRegistration;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysActivityMapper;
import com.volunteer.system.mapper.SysRegistrationMapper;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysActivityService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 报名链路的并发回归测试（真库 + 真多线程，不是 mock）。
 *
 * 为什么必须是集成测试：这里的竞态发生在「数据库行锁 / 条件更新 / 唯一索引」这一层，
 * mock 掉 Mapper 就只能断言「代码调用了哪个方法」，断言不了「并发下真的只生效一次」。
 * 所以本类直连本地 MySQL，用CountDownLatch 让 N 个线程尽量同时冲进同一个方法。
 *
 * 每个用例都会自建 user/activity 并在结束后清理，不依赖也不污染已有数据。
 * 与其他测试一样，需要本地 MySQL 可连接（contextLoads 已有同样的前提）。
 */
@SpringBootTest
class RegistrationConcurrencyTest {

    /** 并发线程数：实测 5 并发足以稳定撞出「先查后改」的竞态 */
    private static final int THREADS = 5;

    @Autowired private SysRegistrationService registrationService;
    @Autowired private SysActivityService activityService;
    @Autowired private SysUserService userService;
    @Autowired private SysRegistrationMapper registrationMapper;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysUserMapper userMapper;

    private Long userId;
    private Long activityId;

    @BeforeEach
    void setUp() {
        String tag = "qa_conc_" + System.nanoTime();

        SysUser user = new SysUser();
        user.setUsername(tag);
        user.setPassword(PasswordUtils.encode("qa-123456"));
        user.setRealName("并发回归测试");
        user.setRole("VOLUNTEER");
        user.setStatus(1);
        user.setCurrentPoints(100);
        user.setTotalPoints(100);
        user.setTotalHours(BigDecimal.ZERO);
        userService.save(user);
        this.userId = user.getUserId();

        SysActivity activity = new SysActivity();
        activity.setTitle(tag);
        activity.setContent("并发回归测试");
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
    @DisplayName("并发取消同一报名：只允许一次生效，名额不会被重复释放")
    void concurrentCancel_releasesSlotOnlyOnce() throws Exception {
        registrationService.applyActivity(userId, activityId);
        Long regId = onlyRegistrationId();
        assertEquals(1, currentNum());

        int success = runConcurrently(() -> registrationService.cancelRegistration(regId, userId));

        assertEquals(1, success, "5 个并发取消请求应当只有 1 个成功");
        assertEquals(4, statusOf(regId), "报名记录应流转到 4-已取消");
        assertEquals(0, currentNum(), "名额只应被释放一次（修复前这里会变成负数）");
    }

    @Test
    @DisplayName("并发审核拒绝同一报名：只允许一次生效，名额不会被重复释放")
    void concurrentReject_releasesSlotOnlyOnce() throws Exception {
        registrationService.applyActivity(userId, activityId);
        Long regId = onlyRegistrationId();

        int success = runConcurrently(() -> registrationService.auditRegistration(regId, 2, "并发回归"));

        assertEquals(1, success, "5 个并发审核请求应当只有 1 个成功");
        assertEquals(2, statusOf(regId), "报名记录应流转到 2-已拒绝");
        assertEquals(0, currentNum(), "名额只应被释放一次");
    }

    @Test
    @DisplayName("并发报名同一活动：只允许一条有效报名（原有防线保持有效）")
    void concurrentApply_createsSingleRegistration() throws Exception {
        int success = runConcurrently(() -> registrationService.applyActivity(userId, activityId));

        assertEquals(1, success, "同一人并发报名应当只有 1 次成功");
        assertEquals(1, currentNum());
        assertEquals(1L, registrationCount());
    }

    @Test
    @DisplayName("取消后名额为 0 时再取消：不会被扣成负数")
    void cancelWhenCounterAlreadyZero_staysAtZero() {
        registrationService.applyActivity(userId, activityId);
        Long regId = onlyRegistrationId();
        registrationService.cancelRegistration(regId, userId);
        assertEquals(0, currentNum());

        // 再取消一次：条件更新应当 0 行，直接失败
        try {
            registrationService.cancelRegistration(regId, userId);
        } catch (Exception expected) {
            // 预期：状态已不是 0/1，闸门拦下
        }

        assertEquals(0, currentNum(), "名额计数不允许低于 0");
    }

    @Test
    @DisplayName("并发签到 / 并发签退同一报名：各只允许一次生效")
    void concurrentSignInAndSignOut_areIdempotent() throws Exception {
        registrationService.applyActivity(userId, activityId);
        Long regId = onlyRegistrationId();
        registrationService.auditRegistration(regId, 1, null);
        startActivityNow();

        int signInSuccess = runConcurrently(() -> registrationService.signIn(regId, userId));
        assertEquals(1, signInSuccess, "5 个并发签到请求应当只有 1 个成功");
        assertEquals(5, statusOf(regId));

        int signOutSuccess = runConcurrently(() -> registrationService.signOut(regId, userId));
        assertEquals(1, signOutSuccess, "5 个并发签退请求应当只有 1 个成功");
        assertEquals(6, statusOf(regId));
    }

    @Test
    @DisplayName("已发放工时的报名不允许再签退：状态机挡住二次发放的入口")
    void signedOutAfterGrant_isRejected() {
        registrationService.applyActivity(userId, activityId);
        Long regId = onlyRegistrationId();
        registrationService.auditRegistration(regId, 1, null);
        startActivityNow();
        registrationService.signIn(regId, userId);
        registrationService.signOut(regId, userId);

        registrationService.grantHours(regId, new BigDecimal("2.00"));
        assertEquals(3, statusOf(regId), "发放后应流转到 3-已完结");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                registrationService.signOut(regId, userId));
        assertEquals(400, ex.getCode(), "已完结的记录不能再签退，否则可再次发放");
        assertEquals(3, statusOf(regId), "状态不应被改回 6");
    }

    // ---------------------------------------------------------------- 工具方法

    /** 让 THREADS 个线程尽量同时执行同一个动作，返回成功（未抛异常）的次数 */
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
                    // 被闸门拦下的请求会抛 ServiceException，属于预期结果；
                    // 其它异常（例如唯一索引之外的数据库错误）留待排查。
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

    private Long onlyRegistrationId() {
        List<SysRegistration> list = registrationMapper.selectList(new LambdaQueryWrapper<SysRegistration>()
                .eq(SysRegistration::getActivityId, activityId)
                .eq(SysRegistration::getUserId, userId));
        assertEquals(1, list.size(), "应当只有一条报名记录");
        return list.get(0).getRegId();
    }

    private long registrationCount() {
        return registrationMapper.selectCount(new LambdaQueryWrapper<SysRegistration>()
                .eq(SysRegistration::getActivityId, activityId)
                .eq(SysRegistration::getUserId, userId));
    }

    private int statusOf(Long regId) {
        return registrationMapper.selectById(regId).getStatus();
    }

    private int currentNum() {
        return activityMapper.selectById(activityId).getCurrentNum();
    }

    /** 把活动切到「进行中」并把开始时间挪到过去，满足签到的时间窗校验 */
    private void startActivityNow() {
        SysActivity activity = activityMapper.selectById(activityId);
        activity.setStatus(1);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activityMapper.updateById(activity);
    }
}
