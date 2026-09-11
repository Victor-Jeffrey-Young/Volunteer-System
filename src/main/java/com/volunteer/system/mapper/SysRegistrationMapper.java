package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRegistrationMapper extends BaseMapper<SysRegistration> {

    /**
     * 加行级锁读取报名记录。
     *
     * 用于「发工时」这类必须只执行一次的结算操作：并发的第二次请求会阻塞在这里，
     * 拿到锁后读到的已经是「已完结(3)」，从而被状态校验拦下 —— 这就是幂等的实现方式，
     * 比起额外维护一张幂等表要轻得多。
     */
    @Select("SELECT * FROM sys_registration WHERE reg_id = #{id} FOR UPDATE")
    SysRegistration selectByIdForUpdate(Long id);
}
