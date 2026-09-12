package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.mapper.SysGoodsMapper;
import com.volunteer.system.service.SysGoodsService;
import org.springframework.stereotype.Service;

/**
 * 积分商品服务实现类
 */
@Service
public class SysGoodsServiceImpl extends ServiceImpl<SysGoodsMapper, SysGoods> implements SysGoodsService {

    /**
     * 实现接口中定义的方法，底层调用 Mapper 手写的 SQL
     */
    @Override
    public SysGoods getByIdForUpdate(Long id) {
        // baseMapper 就是当前 Impl 绑定的 SysGoodsMapper
        return baseMapper.selectByIdForUpdate(id);
    }

    @Override
    public int adjustStock(Long goodsId, Integer delta) {
        return baseMapper.adjustStock(goodsId, delta);
    }
}
