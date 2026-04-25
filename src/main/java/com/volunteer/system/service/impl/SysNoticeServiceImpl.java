package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysNotice;
import com.volunteer.system.mapper.SysNoticeMapper;
import com.volunteer.system.service.SysNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {
    @Override
    public void markAsRead(Long userId, Long noticeId) {
        // 假设您已经创建了 SysNoticeRead 实体和对应的 Mapper
        // 简单的 SQL 执行即可实现“存在则忽略”
        baseMapper.insertReadRecord(userId, noticeId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        // 找出所有公告并插入已读表
        List<SysNotice> list = this.list();
        for (SysNotice notice : list) {
            // 这里建议调用上面的单条标记方法
            this.markAsRead(userId, notice.getNoticeId());
        }
    }

    @Override
    public IPage<SysNotice> getNoticePage(IPage<SysNotice> page, Long userId, String title) {
        // 如果 userId 为空，则所有公告的 isRead 默认为 0
        // 如果 title 有值，SQL 会自动进行 LIKE 模糊查询
        return baseMapper.selectNoticePageWithStatus(page, userId, title);
    }



}