package com.volunteer.system.service;

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
    @DisplayName("场景1：兑换成功 - 资产准确扣减并生成流水")
    void exchange_Success() {
        // 模拟悲观锁查询返回商品，普通查询返回用户
        when(goodsService.getByIdForUpdate(1L)).thenReturn(mockGoods);
        when(userService.getById(101L)).thenReturn(mockUser);
        
        String code = recordService.exchange(101L, 1L);

        // 验证返回结果
        assertNotNull(code);
        assertTrue(code.startsWith("GIFT-"));
        
        // 验证资产扣减
        assertEquals(50, mockUser.getCurrentPoints()); // 200 - 150 = 50
        assertEquals(4, mockGoods.getStock());        // 5 - 1 = 4

        // 验证服务调用：确保调用了数据库更新和保存流水
        verify(userService, times(1)).updateById(mockUser);
        verify(goodsService, times(1)).updateById(mockGoods);
        verify(recordMapper, times(1)).insert(any(SysExchangeRecord.class));
    }

    @Test
    @DisplayName("场景2：兑换失败 - 积分不足 (400)")
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
    @DisplayName("场景4：核销校验 - 权限不足 (403)")
    void verifyExchange_Forbidden() {
        ServiceException ex = assertThrows(ServiceException.class, () -> {
            recordService.verifyExchange("CODE", "VOLUNTEER"); // 志愿者没权限核销
        });
        assertEquals(403, ex.getCode());
    }
}
