package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysExchangeRecord;

public interface SysExchangeRecordService extends IService<SysExchangeRecord>{
    
    /**
     * 核心业务：积分兑换商品
     * @param userId   用户ID（由 JWT 注入，不接受客户端传入）
     * @param goodsId  商品ID
     * @return 兑换成功的核销码
     */
    String exchange(Long userId, Long goodsId);

    /**
     * 核心业务：线下核销兑换码。
     * 管理员权限由 AdminInterceptor 依据 @RequiresAdmin 统一校验，这里不再重复判断角色。
     * @param code     兑换码
     */
    void verifyExchange(String code);
}
