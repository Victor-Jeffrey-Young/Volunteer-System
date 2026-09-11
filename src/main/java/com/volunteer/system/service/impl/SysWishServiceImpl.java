package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.mapper.SysWishMapper;
import com.volunteer.system.service.SysUserService;
import com.volunteer.system.service.SysWishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 微心愿状态机实现。
 *
 * 每个流转方法都遵循同一套「两道闸门」写法：
 *   1. 先读一次记录，把失败原因说清楚（404 / 403 / 409 各司其职）；
 *   2. 再执行带 WHERE 前置条件的 UPDATE，用影响行数确认这次流转真的生效。
 * 只做第 1 步就是经典的「先查后改」竞态；只做第 2 步则无法区分
 * 「不是你的」和「状态不对」，提示会很含糊。两步合起来既准确又安全。
 */
@Slf4j
@Service
public class SysWishServiceImpl extends ServiceImpl<SysWishMapper, SysWish> implements SysWishService {

    /** 0-待审核 */
    private static final int STATUS_PENDING_AUDIT = 0;
    /** 1-展示中(待认领) */
    private static final int STATUS_OPEN = 1;
    /** 2-办理中 */
    private static final int STATUS_IN_PROGRESS = 2;
    /** 4-已驳回 */
    private static final int STATUS_REJECTED = 4;
    /** 5-志愿者已标记完成，等待居民确认 */
    private static final int STATUS_AWAITING_CONFIRM = 5;

    /** 单次结算的奖励上限：防止误填或恶意传参把积分/工时刷爆 */
    private static final int MAX_REWARD_POINTS = 1000;
    private static final BigDecimal MAX_REWARD_HOURS = BigDecimal.valueOf(24);

    @Autowired
    private SysUserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyWish(SysWish wish, Long requesterId) {
        if (requesterId == null) {
            throw new ServiceException(401, "登录状态无效，请重新登录");
        }
        // 发布者一律以 JWT 身份为准。早期版本直接信任请求体里的 requesterId，
        // 任何人都能冒用他人身份发布心愿。
        wish.setWishId(null);          // 主键自增，忽略前端传入的 ID
        wish.setRequesterId(requesterId);
        wish.setVolunteerId(null);     // 认领关系只能由 claimWish 建立
        wish.setStatus(STATUS_PENDING_AUDIT);
        wish.setIsLiked(0);
        wish.setFinishTime(null);
        wish.setCreateTime(LocalDateTime.now());

        this.save(wish);
        log.info("居民 {} 提交了新心愿, ID: {}", requesterId, wish.getWishId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimWish(Long wishId, Long volunteerId) {
        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        if (wish.getStatus() == null || wish.getStatus() != STATUS_OPEN || wish.getVolunteerId() != null) {
            throw new ServiceException(409, "该心愿已不可领或已被抢先认领");
        }
        // 真正的闸门：并发下只有一行能被条件更新命中
        if (baseMapper.claimIfAvailable(wishId, volunteerId) == 0) {
            throw new ServiceException(409, "该心愿已不可领或已被抢先认领");
        }
        log.info("志愿者 {} 认领了心愿 {}", volunteerId, wishId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWish(Long wishId, Long volunteerId) {
        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        if (!volunteerId.equals(wish.getVolunteerId())) {
            throw new ServiceException(403, "只能操作自己认领的心愿");
        }
        if (wish.getStatus() == null || wish.getStatus() != STATUS_IN_PROGRESS) {
            throw new ServiceException(409, "当前状态不可标记完成");
        }
        if (baseMapper.finishIfOwned(wishId, volunteerId) == 0) {
            throw new ServiceException(409, "当前状态不可标记完成");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmWish(Long wishId, Long requesterId, Boolean liked, String rateMsg) {
        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        if (!requesterId.equals(wish.getRequesterId())) {
            throw new ServiceException(403, "只能确认自己发布的心愿");
        }
        if (wish.getStatus() == null || wish.getStatus() != STATUS_AWAITING_CONFIRM) {
            throw new ServiceException(409, "当前状态不可确认");
        }

        String remarks = (rateMsg != null && !rateMsg.trim().isEmpty())
                ? "居民评价: " + rateMsg.trim()
                : null;   // 为空时不覆盖原有备注

        if (baseMapper.confirmIfOwned(wishId, requesterId, remarks) == 0) {
            throw new ServiceException(409, "当前状态不可确认");
        }

        if (Boolean.TRUE.equals(liked) && wish.getVolunteerId() != null) {
            awardLikeOnce(wishId, wish.getVolunteerId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeWish(Long wishId, Long requesterId) {
        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        if (!requesterId.equals(wish.getRequesterId())) {
            throw new ServiceException(403, "只能为自己发布的心愿点赞");
        }
        if (wish.getVolunteerId() == null) {
            throw new ServiceException(400, "该心愿尚无志愿者认领");
        }
        awardLikeOnce(wishId, wish.getVolunteerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleWish(Long wishId, Integer rewardPoints, Double rewardHours) {
        // 参数校验：早期版本对奖励不做任何限制，传负数可以倒扣志愿者资产，
        // 传一个极大的数则能把积分体系直接刷废。
        int points = rewardPoints == null ? 0 : rewardPoints;
        if (points < 0 || points > MAX_REWARD_POINTS) {
            throw new ServiceException(400, "奖励积分需在 0 ~ " + MAX_REWARD_POINTS + " 之间");
        }
        BigDecimal hours = rewardHours == null ? BigDecimal.ZERO : BigDecimal.valueOf(rewardHours);
        if (hours.compareTo(BigDecimal.ZERO) < 0 || hours.compareTo(MAX_REWARD_HOURS) > 0) {
            throw new ServiceException(400, "奖励工时需在 0 ~ " + MAX_REWARD_HOURS + " 小时之间");
        }

        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "心愿不存在");
        }
        if (wish.getVolunteerId() == null) {
            throw new ServiceException(400, "该心愿尚无志愿者认领，无法结算");
        }

        // 条件更新确保「状态流转」只发生一次，重复点击结算不会重复发奖
        if (baseMapper.settleIfSettleable(wishId, points, hours) == 0) {
            throw new ServiceException(409, "该心愿当前状态不可结算，或已被结算过");
        }

        if (userService.addRewards(wish.getVolunteerId(), points, hours) != 1) {
            throw new ServiceException(404, "志愿者账号不存在，结算失败");
        }
        log.info("心愿 {} 结算完成：志愿者 {} 获得 {} 积分 / {} 小时",
                wishId, wish.getVolunteerId(), points, hours);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRejectedWish(Long wishId, Long requesterId) {
        SysWish wish = this.getById(wishId);
        if (wish == null) {
            throw new ServiceException(404, "该记录不存在");
        }
        if (wish.getStatus() == null || wish.getStatus() != STATUS_REJECTED) {
            throw new ServiceException(403, "只能删除被拒绝状态的心愿记录");
        }
        if (!requesterId.equals(wish.getRequesterId())) {
            throw new ServiceException(403, "只能删除自己发布的心愿");
        }
        if (baseMapper.deleteRejectedOwned(wishId, requesterId) == 0) {
            throw new ServiceException(409, "删除失败，请刷新后重试");
        }
    }

    /**
     * 幂等发赞：只有把 is_liked 从 0 翻转到 1 的那一次，才给志愿者累加点赞数。
     * 这样「重复点赞」既不会重复计数，也不会像早期实现那样整体覆盖实体造成丢更新。
     */
    private void awardLikeOnce(Long wishId, Long volunteerId) {
        if (baseMapper.markLikedOnce(wishId) == 0) {
            return;   // 之前已经点过赞
        }
        if (userService.incrementLikes(volunteerId) != 1) {
            log.warn("心愿 {} 点赞成功，但志愿者 {} 不存在，点赞数未累加", wishId, volunteerId);
        }
    }
}
