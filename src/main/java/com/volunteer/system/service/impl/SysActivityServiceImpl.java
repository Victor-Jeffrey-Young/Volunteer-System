package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysActivity;
import com.volunteer.system.mapper.SysActivityMapper;
import com.volunteer.system.service.SysActivityService;
import org.springframework.stereotype.Service;

@Service
public class SysActivityServiceImpl extends ServiceImpl<SysActivityMapper, SysActivity> implements SysActivityService {

    /**
     * 实现接口中定义的方法，底层调用 Mapper 手写的 SQL
     */
    @Override
    public SysActivity getByIdForUpdate(Long activityId) {
        return baseMapper.selectByIdForUpdate(activityId);
    }
}