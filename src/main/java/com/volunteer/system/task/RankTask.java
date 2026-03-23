package com.volunteer.system.task;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Slf4j
@Component
public class RankTask {
    @Autowired
    private SysUserService userService;
    /**
     * 每天凌晨 00:00 执行：将当前的实时排名存入 last_rank 字段
     * 这样用户在白天看到的趋势，就是相对于昨天的变化。
     */
    // 在 RankTask.java 中修改核心逻辑
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void refreshYesterdayRank() {
        log.info("开始执行每日双轨排名快照任务...");

        // 1. 处理积分排名 (存入 last_rank)
        List<SysUser> pointsList = userService.lambdaQuery()
                .eq(SysUser::getRole, "VOLUNTEER")
                .orderByDesc(SysUser::getTotalPoints).list();
        for (int i = 0; i < pointsList.size(); i++) {
            userService.lambdaUpdate()
                    .set(SysUser::getLastRank, i + 1)
                    .eq(SysUser::getUserId, pointsList.get(i).getUserId()).update();
        }

        // 2. 处理时长排名 (存入 last_hours_rank)
        List<SysUser> hoursList = userService.lambdaQuery()
                .eq(SysUser::getRole, "VOLUNTEER")
                .orderByDesc(SysUser::getTotalHours).list();
        for (int i = 0; i < hoursList.size(); i++) {
            userService.lambdaUpdate()
                    .set(SysUser::getLastHoursRank, i + 1)
                    .eq(SysUser::getUserId, hoursList.get(i).getUserId()).update();
        }
    }
}