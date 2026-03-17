package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysExchangeRecord;

public interface SysExchangeRecordService extends IService<SysExchangeRecord>{
    
    /**
     * 核心业务：积分兑换商品
     * @param userId   用户ID
     * @param goodsId  商品ID
     * @return 兑换成功的核销码
     */
    String exchange(Long userId, Long goodsId);

    /**
     * 核心业务：线下核销兑换码
     * @param code     兑换码
     * @param role     操作者角色 (必须为管理员)
     */
    void verifyExchange(String code, String role);
}
