package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysGoodsMapper extends BaseMapper<SysGoods> {

    /**
     * 根据ID查询商品并添加行级锁 (悲观锁)
     * @param id 商品ID
     * @return 商品实体
     */
    @Select("SELECT * FROM sys_goods WHERE goods_id = #{id} FOR UPDATE")
    SysGoods selectByIdForUpdate(Long id);

    /**
     * 按增量调整库存（补货为正、盘亏为负）。
     *
     * 库存是并发资产：兑换链路用 SELECT ... FOR UPDATE + 锁内读改写来扣减，
     * 管理端如果提交「绝对值」就会把并发期间的扣减覆盖掉（实测：库存 1 的商品
     * 被兑换后，管理员提交旧表单把库存写回 1，同一商品成交了 2 单）。
     * 改成增量语义后，管理端的补充与用户的扣减是在同一个值上累加，不会互相覆盖。
     *
     * WHERE 里的 stock + #{delta} >= 0 守卫保证库存不会被调成负数。
     *
     * @return 影响行数，1 表示调整成功，0 表示库存不足或商品不存在
     */
    @Update("UPDATE sys_goods SET stock = stock + #{delta} " +
            "WHERE goods_id = #{goodsId} AND stock + #{delta} >= 0")
    int adjustStock(@Param("goodsId") Long goodsId, @Param("delta") Integer delta);
}
