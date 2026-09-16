package com.volunteer.system.task;

import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysUserMapper;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.utils.PasswordUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排名快照任务的真库回归测试。
 *
 * 原来「查全表 → for 循环逐条更新」的写法只写派生字段，出错也不会错账，
 * 但它把整个循环放在一个事务里，锁窗口覆盖全部志愿者行。改成单条窗口函数 SQL 之后，
 * 需要证明两件事：名次算得对（含并列时的稳定性）、管理员不参与排名。
 *
 * 断言的是「相对顺序」而非绝对名次：库里还有其它志愿者，绝对名次会随环境变化。
 */
@SpringBootTest
class RankTaskTest {

    @Autowired private RankTask rankTask;
    @Autowired private SysUserService userService;
    @Autowired private SysUserMapper userMapper;

    /** 造出来的志愿者：积分 300/200/100，工时 1/3/5（刻意让两个榜单顺序不同） */
    private final List<Long> volunteerIds = new ArrayList<>();
    private Long adminId;

    @BeforeEach
    void setUp() {
        volunteerIds.clear();
        volunteerIds.add(createUser("VOLUNTEER", 300, "1.00"));
        volunteerIds.add(createUser("VOLUNTEER", 200, "3.00"));
        volunteerIds.add(createUser("VOLUNTEER", 100, "5.00"));
        adminId = createUser("ADMIN", 9999, "99.00");
    }

    @AfterEach
    void tearDown() {
        for (Long id : volunteerIds) {
            userMapper.deleteById(id);
        }
        if (adminId != null) {
            userMapper.deleteById(adminId);
        }
    }

    @Test
    @DisplayName("积分榜按积分降序、时长榜按时长降序，且管理员不参与排名")
    void refreshYesterdayRank_ordersByBothMetrics() {
        rankTask.refreshYesterdayRank();

        SysUser first = userMapper.selectById(volunteerIds.get(0));   // 300 分 / 1 小时
        SysUser second = userMapper.selectById(volunteerIds.get(1));  // 200 分 / 3 小时
        SysUser third = userMapper.selectById(volunteerIds.get(2));   // 100 分 / 5 小时

        // 积分榜：300 > 200 > 100，名次必须严格递增
        assertTrue(first.getLastRank() < second.getLastRank(),
                "积分高的名次应更靠前：" + first.getLastRank() + " vs " + second.getLastRank());
        assertTrue(second.getLastRank() < third.getLastRank(),
                "积分高的名次应更靠前：" + second.getLastRank() + " vs " + third.getLastRank());

        // 时长榜：1 < 3 < 5 小时，顺序应与积分榜相反
        assertTrue(third.getLastHoursRank() < second.getLastHoursRank(),
                "工时长的名次应更靠前：" + third.getLastHoursRank() + " vs " + second.getLastHoursRank());
        assertTrue(second.getLastHoursRank() < first.getLastHoursRank(),
                "工时长的名次应更靠前：" + second.getLastHoursRank() + " vs " + first.getLastHoursRank());

        // 管理员不参与排名（与原实现只查 role='VOLUNTEER' 一致）
        SysUser admin = userMapper.selectById(adminId);
        assertEquals(0, admin.getLastRank().intValue(), "管理员不应被写入积分排名");
        assertEquals(0, admin.getLastHoursRank().intValue(), "管理员不应被写入时长排名");
    }

    @Test
    @DisplayName("重复执行是幂等的：指标没变，名次也不变")
    void refreshYesterdayRank_isIdempotent() {
        rankTask.refreshYesterdayRank();
        int firstRun = userMapper.selectById(volunteerIds.get(0)).getLastRank();

        rankTask.refreshYesterdayRank();

        assertEquals(firstRun, userMapper.selectById(volunteerIds.get(0)).getLastRank().intValue(),
                "同一份数据重复跑，名次不应漂移");
    }

    @Test
    @DisplayName("指标变化后重跑：名次随之更新")
    void refreshYesterdayRank_reflectsLatestMetrics() {
        rankTask.refreshYesterdayRank();
        int beforeRank = userMapper.selectById(volunteerIds.get(2)).getLastRank();

        // 把原本积分最低的那位顶到最高
        SysUser boosted = new SysUser();
        boosted.setUserId(volunteerIds.get(2));
        boosted.setTotalPoints(1000);
        userMapper.updateById(boosted);

        rankTask.refreshYesterdayRank();

        int afterRank = userMapper.selectById(volunteerIds.get(2)).getLastRank();
        assertTrue(afterRank < beforeRank,
                "积分被顶高后名次应前进：" + beforeRank + " → " + afterRank);
    }

    private Long createUser(String role, int points, String hours) {
        SysUser user = new SysUser();
        user.setUsername("qa_rank_" + System.nanoTime());
        user.setPassword(PasswordUtils.encode("qa-123456"));
        user.setRealName("排名快照测试");
        user.setRole(role);
        user.setStatus(1);
        user.setCurrentPoints(points);
        user.setTotalPoints(points);
        user.setTotalHours(new BigDecimal(hours));
        userService.save(user);
        return user.getUserId();
    }
}
