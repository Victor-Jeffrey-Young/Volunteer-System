package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 条件取消：只有「本人的、且仍占名额（0-待审 / 1-通过）」的报名才允许取消。
     *
     * 这是取消链路的唯一闸门。早期写法是「getById 读出来 → 判断 status → updateById」，
     * 判断与写入之间没有原子性：并发重复点击时多个请求都会读到 status=0，
     * 于是每一个都去把活动名额 -1，一次取消能释放掉多份名额（实测 5 并发把
     * current_num 从 1 打成了 -4），名额上限随之失效。
     * 把状态前置条件写进 WHERE，返回 0 行就代表「这次没轮到你做状态流转」，
     * 调用方据此不再释放名额。
     */
    @Update("UPDATE sys_registration SET status = 4 " +
            "WHERE reg_id = #{regId} AND user_id = #{userId} AND status IN (0, 1)")
    int cancelIfActive(@Param("regId") Long regId, @Param("userId") Long userId);

    /**
     * 条件审核：只有「待审(0)」的记录能被处理，且目标状态只允许 1-通过 / 2-拒绝。
     *
     * 同样是为了挡住并发/重复点击导致的重复流转（拒绝会连带释放名额）。
     * 状态白名单写在 SQL 里，顺带堵住「审核接口被当成任意状态跳板」的用法
     * （例如直接跳到 6-已签退，再走发工时）。
     * remarks 为空时不覆盖原字段，保持与旧实现一致的「不传就不动」语义。
     */
    @Update("<script>" +
            "UPDATE sys_registration SET status = #{status}, audit_time = NOW()" +
            "<if test='remarks != null'>, remarks = #{remarks}</if>" +
            " WHERE reg_id = #{regId} AND status = 0 AND #{status} IN (1, 2)" +
            "</script>")
    int auditIfPending(@Param("regId") Long regId,
                       @Param("status") Integer status,
                       @Param("remarks") String remarks);
}
