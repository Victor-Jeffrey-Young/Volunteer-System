package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysExchangeRecord;
import com.volunteer.system.mapper.SysExchangeRecordMapper;
import com.volunteer.system.service.SysExchangeRecordService;
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
