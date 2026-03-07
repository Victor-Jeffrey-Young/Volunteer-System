package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.volunteersystem.entity.SysGoods;

public interface SysGoodsService extends IService<SysGoods>{
    /**
     * 根据ID查询并锁定商品记录
     * @param goodsId 商品ID
     * @return 商品实体
     */
    SysGoods getByIdForUpdate(Long goodsId);
}
