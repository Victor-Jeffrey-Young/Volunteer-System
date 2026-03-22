package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.volunteer.system.entity.SysNotice;
import com.volunteer.system.mapper.SysNoticeMapper;
import com.volunteer.system.service.impl.SysNoticeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 社区公告与已读状态模块单元测试
 */
@ExtendWith(MockitoExtension.class)
public class SysNoticeServiceTest {

    @Mock
    private SysNoticeMapper noticeMapper;

    @InjectMocks
    private SysNoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper，因为 ServiceImpl 内部使用的是 baseMapper
        Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(noticeService, noticeMapper);
    }

    @Test
    @DisplayName("场景1：标记单条公告已读 - 验证 DAO 调用正确性")
    void markAsRead_Success() {
        noticeService.markAsRead(1L, 100L);
        // 验证底层 Mapper 的 insertReadRecord 被调用，参数匹配
        verify(noticeMapper, times(1)).insertReadRecord(1L, 100L);
    }

    @Test
    @DisplayName("场景2：一键全部已读 - 批量操作逻辑验证")
    void markAllAsRead_Success() {
        // 1. 模拟系统中存有 3 条公告
        SysNotice n1 = new SysNotice(); n1.setNoticeId(1L);
        SysNotice n2 = new SysNotice(); n2.setNoticeId(2L);
        SysNotice n3 = new SysNotice(); n3.setNoticeId(3L);
        List<SysNotice> mockList = Arrays.asList(n1, n2, n3);

        // 注意：ServiceImpl 的 list() 最终会调用 baseMapper.selectList(any())
        when(noticeMapper.selectList(any())).thenReturn(mockList);

        // 2. 执行批量已读
        noticeService.markAllAsRead(10L); // 用户ID为10

        // 3. 验证对每条公告都发起了已读记录插入请求
        verify(noticeMapper, times(1)).insertReadRecord(10L, 1L);
        verify(noticeMapper, times(1)).insertReadRecord(10L, 2L);
        verify(noticeMapper, times(1)).insertReadRecord(10L, 3L);
        verify(noticeMapper, times(3)).insertReadRecord(anyLong(), anyLong());
    }

    @Test
    @DisplayName("场景3：分页获取带状态的公告 - 验证连表逻辑调用")
    void getNoticePage_WithStatus() {
        IPage<SysNotice> pageReq = new Page<>(1, 10);
        IPage<SysNotice> mockPageResult = new Page<>(1, 10);
        
        when(noticeMapper.selectNoticePageWithStatus(pageReq, 1L, "关键字"))
                .thenReturn(mockPageResult);

        IPage<SysNotice> result = noticeService.getNoticePage(pageReq, 1L, "关键字");

        assertNotNull(result);
        verify(noticeMapper, times(1)).selectNoticePageWithStatus(pageReq, 1L, "关键字");
    }
}
