package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysUser;

import java.math.BigDecimal;

public interface SysUserService extends IService<SysUser> {
    SysUser login(String username, String password);
    void register(SysUser user);

    /**
     * 原子累加积分与工时（发放工时、心愿结算等场景统一走这里）。
     *
     * @return 影响行数，1 表示成功；0 表示用户不存在
     */
    int addRewards(Long userId, int points, BigDecimal hours);

    /**
     * 条件扣减可用积分：余额不足时不产生任何写入。
     *
     * @return 影响行数，1 表示扣减成功；0 表示用户不存在或余额不足
     */
    int deductPointsIfEnough(Long userId, int points);

    /**
     * 原子自增点赞数。
     *
     * @return 影响行数，1 表示成功
     */
    int incrementLikes(Long userId);
}
