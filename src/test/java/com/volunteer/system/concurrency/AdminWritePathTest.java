package com.volunteer.system.concurrency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.system.controller.ActivityController;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @Autowired private SysActivityService activityService;
    @Autowired private SysRegistrationService registrationService;
    @Autowired private SysUserService userService;
    @Autowired private SysActivityMapper activityMapper;
    @Autowired private SysRegistrationMapper registrationMapper;
    @Autowired private SysUserMapper userMapper;

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
}
