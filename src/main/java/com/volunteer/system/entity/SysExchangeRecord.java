package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_exchange_record")
public class SysExchangeRecord {
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long userId;
    private Long goodsId;
    private Integer costPoints;
    private Integer status;
    private LocalDateTime createTime;

    private String redeemCode;
    private LocalDateTime exchangeTime;
    // 这是一个非数据库字段，用于前端展示商品信息
    @TableField(exist = false)
    private String goodsName;
    @TableField(exist = false)
    private String goodsImage;
    @TableField(exist = false)
    private String userName; // 兑换人姓名

}
