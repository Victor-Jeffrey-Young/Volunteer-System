package com.volunteer.system.task;

import com.volunteer.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日排名快照任务。
 *
 * 改造说明（原实现是「一个事务里循环全部志愿者逐条 UPDATE」）：
 *   - 排名交给数据库一条窗口函数语句算完：锁窗口从「遍历全部用户」缩短到「单条 UPDATE」，
 *     午夜与工时结算（grantHours → addRewards 更新同一批 sys_user 行）抢锁的窗口随之消失；
 *   - 去掉方法级 @Transactional：两条语句各自提交。积分榜与时长榜是相互独立的指标，
 *     不需要彼此强一致；若业务确实要求「两个榜单取自同一时刻的快照」，
 *     把 @Transactional 加回来即可（代价是锁窗口重新变长）。
 *   - 名次语义保持不变：ROW_NUMBER() 给出 1..N 不并列，并列时按 user_id 升序保证稳定
 *     （原循环在并列时的先后是不确定的）。若产品想要并列同名次，把 SQL 里的
 *     ROW_NUMBER() 换成 RANK()。
 */
@Slf4j
@Component
public class RankTask {

    @Autowired
    private SysUserService userService;

    /**
     * 每天凌晨 00:00 执行：将当前的实时排名存入 last_rank / last_hours_rank 字段
     * 这样用户在白天看到的趋势，就是相对于昨天的变化。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshYesterdayRank() {
        log.info("开始执行每日双轨排名快照任务...");

        int pointsRows = userService.refreshPointsRank();
        log.info("积分排名快照完成，更新 {} 行", pointsRows);

        int hoursRows = userService.refreshHoursRank();
        log.info("时长排名快照完成，更新 {} 行", hoursRows);
    }
}
