package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysRegistration;

import java.math.BigDecimal;

public interface SysRegistrationService extends IService<SysRegistration> {
    
    /**
     * 志愿者提交报名申请
     */
    void applyActivity(Long userId, Long activityId);

    /**
     * 志愿者取消报名
     */
    void cancelRegistration(Long regId, Long userId);

    /**
     * 志愿者签到打卡
     */
    void signIn(Long regId, Long userId);

    /**
     * 志愿者签退打卡
     */
    void signOut(Long regId, Long userId);

    /**
     * 管理员审核报名
     */
    void auditRegistration(Long regId, Integer status, String remarks);

    /**
     * 管理员发放工时与积分
     */
    void grantHours(Long regId, BigDecimal actualHours);
}