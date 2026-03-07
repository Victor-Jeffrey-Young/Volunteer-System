package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.entity.SysGoods;
import com.volunteer.volunteersystem.mapper.SysGoodsMapper;
import com.volunteer.volunteersystem.service.SysGoodsService;
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

    // 目前无需自定义方法，MyBatis-Plus 的通用方法已足够
    // 未来如果需要复杂的，比如“查询热门兑换商品”，可以在这里写
}
