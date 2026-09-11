package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.entity.SysGoods;
import com.volunteer.system.entity.SysUser;
import com.volunteer.system.mapper.SysExchangeRecordMapper;
import com.volunteer.system.service.impl.SysExchangeRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 积分商城兑换核心逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
public class SysExchangeRecordServiceTest {

    @Mock private SysExchangeRecordMapper recordMapper;
    @Mock private SysUserService userService;
    @Mock private SysGoodsService goodsService;

    @InjectMocks private SysExchangeRecordServiceImpl recordService;

    private SysUser mockUser;
    private SysGoods mockGoods;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 初始化 Mock 数据
        mockUser = new SysUser();
        mockUser.setUserId(101L);
        mockUser.setCurrentPoints(200); // 初始200积分

        mockGoods = new SysGoods();
        mockGoods.setGoodsId(1L);
        mockGoods.setName("测试商品");
        mockGoods.setPointsRequired(150); // 需要150积分
        mockGoods.setStock(5); // 初始5个库存

        // 2. 注入 baseMapper 修复 MyBatis-Plus 内部调用问题
        Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(recordService, recordMapper);
    }

    @Test
    @DisplayName("场景1：兑换成功 - 积分走条件扣减、库存走行锁扣减，并生成随机核销码")
    void exchange_Success() {
        when(goodsService.getByIdForUpdate(1L)).thenReturn(mockGoods);
        when(userService.getById(101L)).thenReturn(mockUser);
        // 条件扣减返回 1 行，代表余额充足且扣减已原子完成
        when(userService.deductPointsIfEnough(101L, 150)).thenReturn(1);
        // 核销码唯一性检查：库里尚无同码
        when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        String code = recordService.exchange(101L, 1L);

        // 验证返回结果
        assertNotNull(code);
        assertTrue(code.startsWith("GIFT-"));
        // 新码是随机串，不应再包含 userId，也不应只有一万种可能
        assertFalse(code.contains("101"), "核销码不应包含用户ID");

        // 验证资产扣减：积分走条件 UPDATE，库存走锁内读改写
        verify(userService, times(1)).deductPointsIfEnough(101L, 150);
        verify(userService, never()).updateById(any(SysUser.class));
        assertEquals(4, mockGoods.getStock());        // 5 - 1 = 4

        // 验证流水落库
        verify(goodsService, times(1)).updateById(mockGoods);
        verify(recordMapper, times(1)).insert(any(SysExchangeRecord.class));
    }

    @Test
    @DisplayName("场景2：兑换失败 - 积分不足 (400)，且不产生任何写入")
    void exchange_NotEnoughPoints() {
        mockUser.setCurrentPoints(50); // 只有50分，买不起150分的商品
        when(goodsService.getByIdForUpdate(1L)).thenReturn(mockGoods);
        when(userService.getById(101L)).thenReturn(mockUser);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            recordService.exchange(101L, 1L);
        });

        assertEquals(400, ex.getCode());
        assertEquals("抱歉，您的可用积分不足", ex.getMessage());

        // 验证未发生任何更新
        verify(userService, never()).deductPointsIfEnough(anyLong(), anyInt());
        verify(userService, never()).updateById(any());
        verify(goodsService, never()).updateById(any());
    }

    @Test
    @DisplayName("场景3：兑换失败 - 库存枯竭 (400)")
    void exchange_OutOfStock() {
        mockGoods.setStock(0); // 没货了
        when(goodsService.getByIdForUpdate(1L)).thenReturn(mockGoods);
        when(userService.getById(101L)).thenReturn(mockUser);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            recordService.exchange(101L, 1L);
        });

        assertEquals(400, ex.getCode());
        assertEquals("抱歉，该商品已被兑换完", ex.getMessage());
    }

    @Test
    @DisplayName("场景4：并发兜底 - 条件扣减返回 0 行时按积分不足处理，不产生写入")
    void exchange_ConcurrentOverdraft_IsRejected() {
        when(goodsService.getByIdForUpdate(1L)).thenReturn(mockGoods);
        when(userService.getById(101L)).thenReturn(mockUser); // 内存里余额看起来够
        // 但原子扣减时发现余额已被并发请求花掉（影响 0 行）
        when(userService.deductPointsIfEnough(101L, 150)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            recordService.exchange(101L, 1L);
        });

        assertEquals(400, ex.getCode());
        assertEquals("抱歉，您的可用积分不足", ex.getMessage());
        verify(recordMapper, never()).insert(any(SysExchangeRecord.class));
    }

    @Test
    @DisplayName("场景5：核销 - 条件更新命中 0 行时判定为重复核销 (400)")
    void verifyExchange_RejectsSecondVerification() {
        SysExchangeRecord used = new SysExchangeRecord();
        used.setRedeemCode("GIFT-ABCDEFGHJK");
        used.setStatus(1); // 已被核销
        when(recordMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(used);

        ServiceException ex = assertThrows(ServiceException.class, () -> {
            recordService.verifyExchange("GIFT-ABCDEFGHJK");
        });

        assertEquals(400, ex.getCode());
        assertEquals("该码已被使用，请勿重复核销", ex.getMessage());
        verify(recordMapper, never()).verifyIfPending(anyString());
    }

    @Test
    @DisplayName("场景6：核销 - 条件更新命中 1 行时核销成功")
    void verifyExchange_SucceedsOnce() {
        SysExchangeRecord pending = new SysExchangeRecord();
        pending.setRedeemCode("GIFT-ABCDEFGHJK");
        pending.setStatus(0); // 待核销
        when(recordMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(pending);
        when(recordMapper.verifyIfPending("GIFT-ABCDEFGHJK")).thenReturn(1);

        assertDoesNotThrow(() -> recordService.verifyExchange("GIFT-ABCDEFGHJK"));

        verify(recordMapper, times(1)).verifyIfPending("GIFT-ABCDEFGHJK");
    }
}
