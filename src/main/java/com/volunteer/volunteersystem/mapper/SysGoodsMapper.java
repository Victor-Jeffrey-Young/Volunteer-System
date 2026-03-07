package com.volunteer.volunteersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.volunteersystem.entity.SysGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysGoodsMapper extends BaseMapper<SysGoods> {

    /**
     * 根据ID查询商品并添加行级锁 (悲观锁)
     * @param id 商品ID
     * @return 商品实体
     */
    @Select("SELECT * FROM sys_goods WHERE goods_id = #{id} FOR UPDATE")
    SysGoods selectByIdForUpdate(Long id);
}
