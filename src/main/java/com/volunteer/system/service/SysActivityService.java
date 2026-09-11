package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysActivity;

public interface SysActivityService extends IService<SysActivity> {

    /**
     * 根据ID查询并锁定活动记录 (悲观锁)
     * @param activityId 活动ID
     * @return 活动实体
     */
    SysActivity getByIdForUpdate(Long activityId);
}