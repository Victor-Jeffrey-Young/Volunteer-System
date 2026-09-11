package com.volunteer.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.system.entity.SysExchangeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysExchangeRecordMapper extends BaseMapper<SysExchangeRecord> {

    /**
     * 条件核销：只有仍处于「待核销(0)」的记录才能被核销。
     * 两个管理员同时扫同一个码时，数据库只会让其中一个命中一行，
     * 另一个拿到 0 行 —— 从根上避免一码两用。
     *
     * @return 影响行数，1 表示本次核销成功
     */
    @Update("UPDATE sys_exchange_record SET status = 1, exchange_time = NOW() " +
            "WHERE redeem_code = #{code} AND status = 0")
    int verifyIfPending(String code);
}
