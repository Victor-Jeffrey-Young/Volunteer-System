package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.entity.SysActivity;
import com.volunteer.volunteersystem.mapper.SysActivityMapper;
import com.volunteer.volunteersystem.service.SysActivityService;
import org.springframework.stereotype.Service;

@Service
public class SysActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements SysActivityService {
}