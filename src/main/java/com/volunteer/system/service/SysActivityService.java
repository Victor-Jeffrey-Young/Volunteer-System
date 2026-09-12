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

    /**
     * 原子释放一个名额（报名被拒绝、用户取消报名时调用）。
     * 带 current_num > 0 守卫，保证计数不会被扣成负数。
     * @param activityId 活动ID
     * @return 影响行数，1 表示确实释放了名额
     */
    int releaseSlot(Long activityId);
}