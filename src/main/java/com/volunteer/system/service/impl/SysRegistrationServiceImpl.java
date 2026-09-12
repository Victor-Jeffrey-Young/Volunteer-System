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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SysRegistrationServiceImpl extends ServiceImpl<SysRegistrationMapper, SysRegistration> implements SysRegistrationService {

    /** 积分折算规则：1 小时 = 10 积分（原来散落在方法体里的魔法数字，统一提为常量） */
    private static final int POINTS_PER_HOUR = 10;
    /** 单次核发工时的上限，防止误填导致排行榜与积分体系失真 */
    private static final BigDecimal MAX_HOURS_PER_GRANT = new BigDecimal("24");

    @Autowired
    private SysActivityService activityService;

    @Autowired
    private SysUserService userService; // 注入 User 服务，用于更新总时长

    // 开启数据库事务，保证报名表和活动表同时更新成功或同时回滚
    // 1. 报名接口 (允许多次报名，保留被拒绝的历史记录)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyActivity(Long userId, Long activityId) {
        // 悲观锁锁住活动行：同一活动的所有报名请求在此串行化，
        // 后到的请求能读到最新已提交数据，杜绝并发判重穿透与名额超卖
        SysActivity activity = activityService.getByIdForUpdate(activityId);
        if (activity == null || activity.getStatus() != 0) throw new ServiceException(404, "活动不存在或已停止招募");
        if (activity.getCurrentNum() >= activity.getCapacity()) throw new ServiceException(400, "名额已满！");

        // 有效状态判重 (0待审/1通过/3完结/5已签到/6已签退)；
        // 2(拒绝)、4(取消) 不占用名额，允许重新报名
        LambdaQueryWrapper<SysRegistration> query = new LambdaQueryWrapper<>();
        query.eq(SysRegistration::getUserId, userId)
                .eq(SysRegistration::getActivityId, activityId)
                .in(SysRegistration::getStatus, 0, 1, 3, 5, 6);

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
        try {
            this.save(reg);
        } catch (DuplicateKeyException e) {
            // 数据库唯一索引兜底 (user_id + activity_id + 有效状态生成列)：
            // 锁内判重理论上已拦住，这里防御任何残余竞态
            throw new ServiceException(409, "您当前已有该活动的有效报名，请勿重复操作！");
        }

        activity.setCurrentNum(activity.getCurrentNum() + 1);
        activityService.updateById(activity);
    }

    // 2. 取消报名逻辑
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long regId, Long userId) {
        SysRegistration reg = this.getById(regId);
        if (reg == null || reg.getUserId() == null || !reg.getUserId().equals(userId)) {
            throw new ServiceException(403, "非法操作");
        }

        // 先锁活动行，再对报名行做条件流转：与 applyActivity 使用同一把锁、
        // 同一个加锁顺序（活动行 → 报名行），两条路径不会互相死锁。
        if (activityService.getByIdForUpdate(reg.getActivityId()) == null) {
            throw new ServiceException(404, "活动不存在");
        }

        // 唯一闸门：只有「仍占名额」的那一次取消能命中一行。
        // 并发重复点击时其余请求拿到 0 行直接失败，不会再去释放名额。
        if (baseMapper.cancelIfActive(regId, userId) == 0) {
            throw new ServiceException(400, "当前状态无法取消报名");
        }

        // 释放名额：一条带 current_num > 0 守卫的原子语句，不会把计数扣成负数
        activityService.releaseSlot(reg.getActivityId());
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
        // 入参校验：早期版本完全不校验，传负数可以「倒扣」他人工时积分，
        // 传天文数字则能把排行榜直接刷废
        if (actualHours == null || actualHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(400, "核发工时必须大于 0");
        }
        if (actualHours.compareTo(MAX_HOURS_PER_GRANT) > 0) {
            throw new ServiceException(400, "单次核发工时不能超过 " + MAX_HOURS_PER_GRANT + " 小时");
        }

        // 锁住报名行：并发重复结算时后到的事务会阻塞在这里，
        // 拿到锁后读到的是「已完结(3)」，被下面的状态校验拦下 —— 这就是幂等的实现，
        // 不需要额外维护一张幂等表。
        SysRegistration reg = baseMapper.selectByIdForUpdate(regId);
        if (reg == null) {
            throw new ServiceException(404, "报名记录不存在");
        }
        if (reg.getStatus() == null || (reg.getStatus() != 6 && reg.getStatus() != 5 && reg.getStatus() != 1)) {
            throw new ServiceException(400, "当前状态无法发放工时");
        }

        // 计算本次应发积分 (1 小时 = 10 积分)。
        // 用 multiply 而不是 intValue() * 10：后者会先把 1.9 小时截断成 1 小时，少发积分。
        int earnedPoints = actualHours.multiply(BigDecimal.valueOf(POINTS_PER_HOUR)).intValue();

        reg.setStatus(3);                   // 3-完结
        reg.setActualHours(actualHours);
        reg.setRewardPoints(earnedPoints);  // 记录本次获得的积分
        this.updateById(reg);

        // 原子累加用户资产（可用积分 + 总积分 + 总工时），避免并发读改写丢更新，
        // 也省掉了原来对 totalHours / totalPoints 可能为 null 的手工兜底。
        if (userService.addRewards(reg.getUserId(), earnedPoints, actualHours) != 1) {
            throw new ServiceException(404, "用户不存在，工时结算失败");
        }
    }

    /**
     * 管理员审核报名 (通过/拒绝)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRegistration(Long regId, Integer status, String remarks) {
        // 状态白名单：审核接口只负责「待审 → 通过/拒绝」，
        // 不允许被当成任意状态跳板（例如直接跳到 6-已签退再走发工时）
        if (status == null || (status != 1 && status != 2)) {
            throw new ServiceException(400, "审核结果只能是 1-通过 或 2-拒绝");
        }

        SysRegistration reg = this.getById(regId);
        if (reg == null) {
            throw new ServiceException(400, "记录不存在");
        }

        // 与报名/取消共用活动行锁，保证加锁顺序一致
        activityService.getByIdForUpdate(reg.getActivityId());

        // 条件流转：并发重复审核只会生效一次，拒绝带来的名额释放也就只会发生一次
        if (baseMapper.auditIfPending(regId, status, remarks) == 0) {
            throw new ServiceException(400, "记录不存在或已处理过");
        }

        // 拒绝时释名额：原子语句 + current_num > 0 守卫
        if (status == 2) {
            activityService.releaseSlot(reg.getActivityId());
        }
    }
}