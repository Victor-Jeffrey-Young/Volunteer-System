package com.volunteer.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.system.entity.SysNotice;

public interface SysNoticeService extends IService<SysNotice> {

    void markAllAsRead(Long userId);

    void markAsRead(Long userId, Long id);

    IPage<SysNotice> getNoticePage(IPage<SysNotice> page, Long userId, String title);
}