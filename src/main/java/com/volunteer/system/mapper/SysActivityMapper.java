package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysActivityMapper extends BaseMapper<SysActivity> {

    /**
     * 根据ID查询活动并添加行级锁 (悲观锁)
     * 用于报名/取消/审核等需要修改 current_num 的场景，
     * 防止并发下的名额超卖与重复报名
     * @param id 活动ID
     * @return 活动实体
     */
    @Select("SELECT * FROM sys_activity WHERE activity_id = #{id} FOR UPDATE")
    SysActivity selectByIdForUpdate(Long id);

    /**
     * 原子释放一个名额（拒绝、取消报名时调用）。
     *
     * 把「减一」交给数据库一条语句完成，调用方不再需要「读出来减一再写回」。
     * WHERE 里带 current_num > 0 守卫，是计数器的最后一道防线：
     * 即使上游因为并发或历史脏数据多释放了一次，名额也只会落到 0，不会变成负数
     * （负数会让「current_num >= capacity」的满员判断彻底失效，等于放开超额报名）。
     *
     * @return 影响行数，1 表示确实释放了一个名额
     */
    @Update("UPDATE sys_activity SET current_num = current_num - 1 " +
            "WHERE activity_id = #{id} AND current_num > 0")
    int releaseSlot(Long id);
}