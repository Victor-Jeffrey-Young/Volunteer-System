package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.mapper.SysActivityMapper;
import com.volunteer.system.service.SysActivityService;
import org.springframework.stereotype.Service;

@Service
public class SysActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements SysActivityService {
}