package com.volunteer.volunteersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.volunteersystem.entity.SysExchangeRecord;
import com.volunteer.volunteersystem.mapper.SysExchangeRecordMapper;
import com.volunteer.volunteersystem.service.SysExchangeRecordService;
import org.springframework.stereotype.Service;

/**
 * 兑换记录服务实现类
 */
@Service
public class SysExchangeRecordServiceImpl
        extends ServiceImpl<SysExchangeRecordMapper, SysExchangeRecord>
        implements SysExchangeRecordService {

    // 同样，目前无需自定义方法
}
