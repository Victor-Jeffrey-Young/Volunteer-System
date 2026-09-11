package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}