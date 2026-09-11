package com.volunteer.system.service;

import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.mapper.SysWishMapper;
import com.volunteer.system.service.impl.SysWishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 微心愿状态机单元测试。
 *
 * 覆盖的是「权限 + 状态流转」这两类业务规则 —— 它们原本散落在 Controller 里，
 * 现在全部收敛在 Service 层，因此测试也写在 Service 层。
 */
@ExtendWith(MockitoExtension.class)
public class SysWishServiceTest {

    private static final long WISH_ID = 200L;
    private static final long REQUESTER_ID = 5L;    // 居民：心愿发布人
    private static final long VOLUNTEER_ID = 10L;   // 志愿者：认领人
    private static final long OTHER_USER_ID = 77L;  // 无关的第三方

    @Mock private SysWishMapper wishMapper;
    @Mock private SysUserService userService;

    @InjectMocks private SysWishServiceImpl wishService;

    @BeforeEach
    void setUp() throws Exception {
        Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(wishService, wishMapper);
    }

    private SysWish wish(int status, Long requesterId, Long volunteerId) {
        SysWish w = new SysWish();
        w.setWishId(WISH_ID);
        w.setStatus(status);
        w.setRequesterId(requesterId);
        w.setVolunteerId(volunteerId);
        w.setIsLiked(0);
        return w;
    }

    // ---------------- 发布 ----------------

    @Test
    @DisplayName("发布心愿：requesterId 以 JWT 身份为准，请求体传入的 ID 被覆盖，主键强制自增")
    void applyWish_OverridesRequesterIdFromBody() {
        SysWish forged = new SysWish();
        forged.setWishId(999L);          // 试图指定主键
        forged.setRequesterId(OTHER_USER_ID); // 试图冒用他人身份
        forged.setVolunteerId(OTHER_USER_ID);
        forged.setStatus(3);             // 试图直接落成已达成

        wishService.applyWish(forged, REQUESTER_ID);

        assertNull(forged.getWishId(), "主键应被清空以走自增");
        assertEquals(REQUESTER_ID, forged.getRequesterId(), "发布者必须是 JWT 身份");
        assertNull(forged.getVolunteerId(), "认领关系不能由发布接口预设");
        assertEquals(0, forged.getStatus(), "新心愿一律从待审核开始");
        assertNotNull(forged.getCreateTime());
        verify(wishMapper).insert(forged);
    }

    // ---------------- 认领 ----------------

    @Test
    @DisplayName("认领成功：条件更新命中 1 行")
    void claimWish_Succeeds() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(1, REQUESTER_ID, null));
        when(wishMapper.claimIfAvailable(WISH_ID, VOLUNTEER_ID)).thenReturn(1);

        assertDoesNotThrow(() -> wishService.claimWish(WISH_ID, VOLUNTEER_ID));
    }

    @Test
    @DisplayName("并发认领：条件更新命中 0 行时返回 409，不会覆盖先到的志愿者")
    void claimWish_AlreadyClaimed_Returns409() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(1, REQUESTER_ID, null));
        when(wishMapper.claimIfAvailable(WISH_ID, VOLUNTEER_ID)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.claimWish(WISH_ID, VOLUNTEER_ID));

        assertEquals(409, ex.getCode());
    }

    @Test
    @DisplayName("认领不存在的心愿 → 404")
    void claimWish_NotFound() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.claimWish(WISH_ID, VOLUNTEER_ID));

        assertEquals(404, ex.getCode());
        verify(wishMapper, never()).claimIfAvailable(anyLong(), anyLong());
    }

    // ---------------- 标记完成 ----------------

    @Test
    @DisplayName("标记完成：非认领人操作 → 403，且不触发任何更新")
    void finishWish_NotOwner_Returns403() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(2, REQUESTER_ID, VOLUNTEER_ID));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.finishWish(WISH_ID, OTHER_USER_ID));

        assertEquals(403, ex.getCode());
        verify(wishMapper, never()).finishIfOwned(anyLong(), anyLong());
    }

    @Test
    @DisplayName("标记完成：状态不是办理中 → 409")
    void finishWish_WrongStatus_Returns409() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(1, REQUESTER_ID, VOLUNTEER_ID));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.finishWish(WISH_ID, VOLUNTEER_ID));

        assertEquals(409, ex.getCode());
    }

    @Test
    @DisplayName("标记完成：本人 + 办理中 → 成功")
    void finishWish_Succeeds() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(2, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.finishIfOwned(WISH_ID, VOLUNTEER_ID)).thenReturn(1);

        assertDoesNotThrow(() -> wishService.finishWish(WISH_ID, VOLUNTEER_ID));
    }

    // ---------------- 确认 ----------------

    @Test
    @DisplayName("确认完成：非发布人操作他人心愿 → 403（水平越权拦截）")
    void confirmWish_NotRequester_Returns403() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(5, REQUESTER_ID, VOLUNTEER_ID));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.confirmWish(WISH_ID, OTHER_USER_ID, true, "好评"));

        assertEquals(403, ex.getCode());
        verify(wishMapper, never()).confirmIfOwned(anyLong(), anyLong(), any());
        verify(userService, never()).incrementLikes(anyLong());
    }

    @Test
    @DisplayName("确认并点赞：发布人本人操作 → 状态流转且志愿者点赞数 +1（只加一次）")
    void confirmWish_WithLike_IncrementsLikesOnce() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(5, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.confirmIfOwned(eq(WISH_ID), eq(REQUESTER_ID), eq("居民评价: 非常感谢"))).thenReturn(1);
        when(wishMapper.markLikedOnce(WISH_ID)).thenReturn(1);
        when(userService.incrementLikes(VOLUNTEER_ID)).thenReturn(1);

        wishService.confirmWish(WISH_ID, REQUESTER_ID, true, "非常感谢");

        verify(userService, times(1)).incrementLikes(VOLUNTEER_ID);
    }

    @Test
    @DisplayName("确认时未点赞：不产生点赞计数")
    void confirmWish_WithoutLike_DoesNotIncrement() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(5, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.confirmIfOwned(eq(WISH_ID), eq(REQUESTER_ID), eq(null))).thenReturn(1);

        wishService.confirmWish(WISH_ID, REQUESTER_ID, false, null);

        verify(userService, never()).incrementLikes(anyLong());
        verify(wishMapper, never()).markLikedOnce(anyLong());
    }

    @Test
    @DisplayName("重复确认：条件更新命中 0 行 → 409")
    void confirmWish_AlreadyConfirmed_Returns409() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(5, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.confirmIfOwned(anyLong(), anyLong(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.confirmWish(WISH_ID, REQUESTER_ID, false, null));

        assertEquals(409, ex.getCode());
    }

    // ---------------- 点赞 ----------------

    @Test
    @DisplayName("点赞幂等：已点过赞时不再重复累加志愿者点赞数")
    void likeWish_IsIdempotent() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(3, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.markLikedOnce(WISH_ID)).thenReturn(0);  // 状态已翻转过

        wishService.likeWish(WISH_ID, REQUESTER_ID);

        verify(userService, never()).incrementLikes(anyLong());
    }

    @Test
    @DisplayName("点赞：非发布人 → 403")
    void likeWish_NotRequester_Returns403() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(3, REQUESTER_ID, VOLUNTEER_ID));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.likeWish(WISH_ID, OTHER_USER_ID));

        assertEquals(403, ex.getCode());
        verify(userService, never()).incrementLikes(anyLong());
    }

    // ---------------- 结算 ----------------

    @Test
    @DisplayName("结算：负积分被拒 → 400（旧实现允许倒扣志愿者资产）")
    void settleWish_NegativePoints_Returns400() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.settleWish(WISH_ID, -100, 1.0));

        assertEquals(400, ex.getCode());
        verify(userService, never()).addRewards(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("结算：积分超上限被拒 → 400")
    void settleWish_ExcessivePoints_Returns400() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.settleWish(WISH_ID, 999999, 1.0));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("结算：工时超上限被拒 → 400")
    void settleWish_ExcessiveHours_Returns400() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.settleWish(WISH_ID, 10, 100.0));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("结算成功：奖励通过原子方法累加，且只执行一次状态流转")
    void settleWish_Succeeds() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(6, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.settleIfSettleable(eq(WISH_ID), eq(50), eq(BigDecimal.valueOf(2.0)))).thenReturn(1);
        when(userService.addRewards(VOLUNTEER_ID, 50, BigDecimal.valueOf(2.0))).thenReturn(1);

        wishService.settleWish(WISH_ID, 50, 2.0);

        verify(userService, times(1)).addRewards(VOLUNTEER_ID, 50, BigDecimal.valueOf(2.0));
    }

    @Test
    @DisplayName("重复结算：条件更新命中 0 行 → 409，且不再发奖")
    void settleWish_AlreadySettled_Returns409() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(6, REQUESTER_ID, VOLUNTEER_ID));
        when(wishMapper.settleIfSettleable(anyLong(), anyInt(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.settleWish(WISH_ID, 50, 2.0));

        assertEquals(409, ex.getCode());
        verify(userService, never()).addRewards(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("结算：无人认领的心愿不可结算 → 400")
    void settleWish_NoVolunteer_Returns400() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(6, REQUESTER_ID, null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.settleWish(WISH_ID, 50, 2.0));

        assertEquals(400, ex.getCode());
    }

    // ---------------- 删除 ----------------

    @Test
    @DisplayName("删除心愿：非发布人 → 403")
    void deleteRejectedWish_NotOwner_Returns403() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(4, REQUESTER_ID, null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.deleteRejectedWish(WISH_ID, OTHER_USER_ID));

        assertEquals(403, ex.getCode());
        verify(wishMapper, never()).deleteRejectedOwned(anyLong(), anyLong());
    }

    @Test
    @DisplayName("删除心愿：非驳回状态 → 403")
    void deleteRejectedWish_WrongStatus_Returns403() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(1, REQUESTER_ID, null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> wishService.deleteRejectedWish(WISH_ID, REQUESTER_ID));

        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("删除心愿：本人 + 已驳回 → 成功")
    void deleteRejectedWish_Succeeds() {
        when(wishMapper.selectById(WISH_ID)).thenReturn(wish(4, REQUESTER_ID, null));
        when(wishMapper.deleteRejectedOwned(WISH_ID, REQUESTER_ID)).thenReturn(1);

        assertDoesNotThrow(() -> wishService.deleteRejectedWish(WISH_ID, REQUESTER_ID));
    }
}
