package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysGoods;

public interface SysGoodsService extends IService<SysGoods>{
    /**
     * 根据ID查询并锁定商品记录
     * @param goodsId 商品ID
     * @return 商品实体
     */
    SysGoods getByIdForUpdate(Long goodsId);

    /**
     * 按增量调整库存（补货为正、盘亏为负），带「库存不得为负」守卫。
     * @param goodsId 商品ID
     * @param delta 调整数量
     * @return 影响行数，1 表示调整成功，0 表示库存不足或商品不存在
     */
    int adjustStock(Long goodsId, Integer delta);
}
