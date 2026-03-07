package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.volunteersystem.entity.SysRegistration;

import java.math.BigDecimal;

public interface SysRegistrationService extends IService<SysRegistration> {
    void applyActivity(Long userId, Long activityId);

    void grantHours(Long regId, BigDecimal actualHours);

}