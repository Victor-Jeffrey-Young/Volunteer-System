package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysWish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 微心愿 Mapper。
 *
 * 这里的写操作统一采用「条件更新（compare-and-set）」而不是「先查状态再 updateById」：
 * 把状态前置条件与归属校验收进 UPDATE 的 WHERE 子句，由数据库保证原子性，
 * 返回值即影响行数 —— 0 行就代表「并发中被别人抢先」或「本来就不该操作」。
 * 这一写法同时解决了重复认领、重复确认、重复点赞三类竞态。
 */
@Mapper
public interface SysWishMapper extends BaseMapper<SysWish> {

    /**
     * 条件审核：只有「待审核(0)」的心愿能被审核，目标状态限定 1-展示中 / 4-已驳回。
     *
     * 审核是状态机的入口，必须同样遵守「先查后改要原子」的规矩：
     * 没有前置条件时，管理员对着已认领/已结算的心愿再点一次审核就能强行改状态，
     * 造成认领关系与状态不一致（实测：status=2 且 volunteer_id 非空的心愿被改成 4，
     * 之后既不能被认领也不能被结算，心愿卡死）。
     * remarks 为空时不覆盖原字段。
     */
    @Update("<script>" +
            "UPDATE sys_wish SET status = #{status}" +
            "<if test='remarks != null'>, remarks = #{remarks}</if>" +
            " WHERE wish_id = #{wishId} AND status = 0 AND #{status} IN (1, 4)" +
            "</script>")
    int auditIfPending(@Param("wishId") Long wishId,
                       @Param("status") Integer status,
                       @Param("remarks") String remarks);

    /**
     * 条件认领：只有「展示中(1) 且尚无志愿者」才允许认领。
     * 两个志愿者同时点认领时，数据库只会让其中一个 UPDATE 命中一行。
     */
    @Update("UPDATE sys_wish SET volunteer_id = #{volunteerId}, status = 2 " +
            "WHERE wish_id = #{wishId} AND status = 1 AND volunteer_id IS NULL")
    int claimIfAvailable(@Param("wishId") Long wishId,
                         @Param("volunteerId") Long volunteerId);

    /** 条件标记完成：只有「办理中(2) 且是该志愿者本人认领的」才流转到 5-待确认 */
    @Update("UPDATE sys_wish SET status = 5 " +
            "WHERE wish_id = #{wishId} AND status = 2 AND volunteer_id = #{volunteerId}")
    int finishIfOwned(@Param("wishId") Long wishId,
                      @Param("volunteerId") Long volunteerId);

    /**
     * 条件确认：只有「待确认(5) 且请求者是心愿发布人本人」才流转到 6-待结算。
     * remarks 为空时不覆盖原字段，避免把管理员的审核意见擦掉。
     */
    @Update("<script>" +
            "UPDATE sys_wish SET status = 6" +
            "<if test='remarks != null'>, remarks = #{remarks}</if>" +
            " WHERE wish_id = #{wishId} AND status = 5 AND requester_id = #{requesterId}" +
            "</script>")
    int confirmIfOwned(@Param("wishId") Long wishId,
                       @Param("requesterId") Long requesterId,
                       @Param("remarks") String remarks);

    /**
     * 幂等点赞标记：仅当 is_liked 还是 0 时置 1。
     * 返回 1 才代表「本次真的新增了一个赞」，调用方据此才去给志愿者 +1，
     * 于是重复点赞既不会重复计数，也不会丢计数。
     */
    @Update("UPDATE sys_wish SET is_liked = 1 WHERE wish_id = #{wishId} AND is_liked = 0")
    int markLikedOnce(@Param("wishId") Long wishId);

    /** 条件结算：只有「办理中(2) 或 待结算(6) 且已有志愿者」才流转到 3-已达成 */
    @Update("UPDATE sys_wish SET status = 3, reward_points = #{points}, " +
            "reward_hours = #{hours}, finish_time = NOW() " +
            "WHERE wish_id = #{wishId} AND status IN (2, 6) AND volunteer_id IS NOT NULL")
    int settleIfSettleable(@Param("wishId") Long wishId,
                           @Param("points") int points,
                           @Param("hours") BigDecimal hours);

    /** 条件删除：只有「被驳回(4) 且是发布人本人」的心愿允许删除 */
    @Delete("DELETE FROM sys_wish WHERE wish_id = #{wishId} AND status = 4 AND requester_id = #{requesterId}")
    int deleteRejectedOwned(@Param("wishId") Long wishId,
                            @Param("requesterId") Long requesterId);
}
