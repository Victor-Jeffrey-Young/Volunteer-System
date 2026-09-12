package com.volunteer.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysWish;

/**
 * 微心愿业务接口。
 *
 * 状态机（0-待审核 → 1-展示中 → 2-办理中 → 5-待确认 → 6-待结算 → 3-已达成，4-已驳回）
 * 的流转规则与归属校验全部收敛在实现类里，Controller 只负责取参数和返回结果。
 * 这样做的原因：权限校验（谁能操作）和状态校验（此刻能不能操作）都属于业务规则，
 * 散落在 Controller 里就一定会出现「某个接口忘了校验」。
 */
public interface SysWishService extends IService<SysWish> {

    /** 居民提交心愿：发布者身份由服务端注入，不接受前端传入 */
    void applyWish(SysWish wish, Long requesterId);

    /** 管理员审核心愿：只有待审核(0)的记录能流转到 1-展示中 / 4-已驳回 */
    void auditWish(Long wishId, Integer status, String remarks);

    /** 志愿者认领心愿（并发下只有一个能成功） */
    void claimWish(Long wishId, Long volunteerId);

    /** 志愿者标记服务完成：只能操作自己认领的心愿 */
    void finishWish(Long wishId, Long volunteerId);

    /** 居民确认完成并可选点赞：只能确认自己发布的心愿 */
    void confirmWish(Long wishId, Long requesterId, Boolean liked, String rateMsg);

    /** 居民追加点赞：幂等，重复调用不会重复计数 */
    void likeWish(Long wishId, Long requesterId);

    /** 管理员结算并核发奖励（积分与工时原子累加） */
    void settleWish(Long wishId, Integer rewardPoints, Double rewardHours);

    /** 居民删除自己被驳回的心愿 */
    void deleteRejectedWish(Long wishId, Long requesterId);
}
