package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.entity.SysRegistration;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysRegistrationMapper;
import com.volunteer.system.service.SysActivityService;
import com.volunteer.system.service.SysRegistrationService;
import com.volunteer.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SysRegistrationServiceImpl extends ServiceImpl<SysRegistrationMapper, SysRegistration> implements SysRegistrationService {

    @Autowired
    private SysActivityService activityService;

    @Autowired
    private SysUserService userService; // 注入 User 服务，用于更新总时长

    // 开启数据库事务，保证报名表和活动表同时更新成功或同时回滚
    // 1. 报名接口 (允许多次报名，保留被拒绝的历史记录)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyActivity(Long userId, Long activityId) {
        SysActivity activity = activityService.getById(activityId);
        if (activity == null || activity.getStatus() != 0) throw new ServiceException(404, "活动不存在或已停止招募");
        if (activity.getCurrentNum() >= activity.getCapacity()) throw new ServiceException(400, "名额已满！");

        // 只拦截那些“正在进行中”的状态 (0,1,3,5)
        LambdaQueryWrapper<SysRegistration> query = new LambdaQueryWrapper<>();
        query.eq(SysRegistration::getUserId, userId)
                .eq(SysRegistration::getActivityId, activityId)
                .in(SysRegistration::getStatus, 0, 1, 3, 5); // 排除 2(拒绝) 和 4(取消)

        if (this.count(query) > 0) {
            throw new ServiceException(409, "您当前已有该活动的有效报名，请勿重复操作！");
        }

        // 每次报名都插入一条【新】记录，保留失败历史
        SysRegistration reg = new SysRegistration();
        reg.setUserId(userId);
        reg.setActivityId(activityId);
        reg.setStatus(0);
        reg.setApplyTime(LocalDateTime.now());
        reg.setActualHours(new java.math.BigDecimal("0.00"));
        this.save(reg);

        activity.setCurrentNum(activity.getCurrentNum() + 1);
        activityService.updateById(activity);
    }

    // 2. 取消报名逻辑
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long regId, Long userId) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || !reg.getUserId().equals(userId)) throw new ServiceException(403, "非法操作");
        if (reg.getStatus() == 0 || reg.getStatus() == 1) {
            reg.setStatus(4); // 4-已取消
            this.updateById(reg);

            // 释放名额
            SysActivity activity = activityService.getById(reg.getActivityId());
            activity.setCurrentNum(activity.getCurrentNum() - 1);
            activityService.updateById(activity);
        } else {
            throw new ServiceException(400, "当前状态无法取消报名");
        }
    }

    // 3. 签到打卡 (开始)
    public void signIn(Long regId, Long userId) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || !reg.getUserId().equals(userId)) throw new ServiceException(403, "非法操作");

        // 1. 校验报名状态
        if (reg.getStatus() != 1) throw new ServiceException(400, "只有【审核通过】的状态才能签到");

        // 2. 校验活动状态和时间
        SysActivity activity = activityService.getById(reg.getActivityId());
        if (activity == null) throw new ServiceException(404, "活动不存在");

        // 逻辑 A：必须是“进行中”状态 (Status = 1)
        if (activity.getStatus() != 1) {
            // 如果是招募中(0)，提示未开始
            if (activity.getStatus() == 0) throw new ServiceException(400, "活动尚未开始，请等待管理员开启活动！");
            // 如果是已结束(2)或取消(3)
            throw new ServiceException(400, "活动已结束或取消，无法签到");
        }

        // 逻辑 B：(可选) 必须到达开始时间 (例如：允许提前 30 分钟签到)
        LocalDateTime canSignInTime = activity.getStartTime().minusMinutes(30);
        if (LocalDateTime.now().isBefore(canSignInTime)) {
            throw new ServiceException(400, "未到签到时间，请在活动开始前30分钟内签到");
        }

        // 3. 执行签到
        reg.setStatus(5); // 5-已签到
        reg.setSignInTime(LocalDateTime.now());
        this.updateById(reg);
    }

    // 4. 签退打卡 (结束)
    public void signOut(Long regId, Long userId) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || !reg.getUserId().equals(userId)) throw new ServiceException(403, "非法操作");
        if (reg.getStatus() != 5) throw new ServiceException(400, "请先进行签到打卡！");

        reg.setStatus(6); // 6-已签退(待发工时)
        reg.setSignOutTime(LocalDateTime.now()); // 记录签退时间
        this.updateById(reg);
    }

    // 5. 发放工时与积分
    @Transactional(rollbackFor = Exception.class)
    public void grantHours(Long regId, BigDecimal actualHours) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || (reg.getStatus() != 6 && reg.getStatus() != 5 && reg.getStatus() != 1)) {
            throw new ServiceException(400, "当前状态无法发放工时");
        }

        // 计算本次应发积分 (假设 1小时 = 10积分)
        int earnedPoints = actualHours.intValue() * 10;

        reg.setStatus(3);                   // 3-完结
        reg.setActualHours(actualHours);
        reg.setRewardPoints(earnedPoints);  // 记录本次获得的积分
        this.updateById(reg);

        SysUser user = userService.getById(reg.getUserId());
        if (user != null) {
            user.setTotalHours(user.getTotalHours().add(actualHours));
            // 双积分同时增加
            user.setTotalPoints(user.getTotalPoints() + earnedPoints);
            user.setCurrentPoints(user.getCurrentPoints() + earnedPoints);
            userService.updateById(user);
        }
    }

    /**
     * 管理员审核报名 (通过/拒绝)
     */
    public void auditRegistration(Long regId, Integer status, String remarks) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || reg.getStatus() != 0) {
            throw new ServiceException(400, "记录不存在或已处理过");
        }
        reg.setStatus(status); // 1-通过, 2-拒绝
        reg.setAuditTime(LocalDateTime.now());
        reg.setRemarks(remarks);
        this.updateById(reg);

        // 扩展逻辑：如果拒绝了，活动的已招募人数应该 -1 释放名额
        if (status == 2) {
            SysActivity activity = activityService.getById(reg.getActivityId());
            if (activity != null && activity.getCurrentNum() > 0) {
                activity.setCurrentNum(activity.getCurrentNum() - 1);
                activityService.updateById(activity);
            }
        }
    }
}