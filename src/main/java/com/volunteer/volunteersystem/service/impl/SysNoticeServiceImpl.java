package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.entity.SysNotice;
import com.volunteer.volunteersystem.mapper.SysNoticeMapper;
import com.volunteer.volunteersystem.service.SysNoticeService;
import org.springframework.stereotype.Service;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {}