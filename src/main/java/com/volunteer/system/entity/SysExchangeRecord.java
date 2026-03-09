package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_exchange_record")
@Schema(description = "积分商城兑换流水与核销审计表", name = "SysExchangeRecord")
public class SysExchangeRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "兑换流水ID")
    private Long recordId;

    @Schema(description = "发起兑换的用户ID")
    private Long userId;

    @Schema(description = "被兑换的商品ID")
    private Long goodsId;

    @Schema(description = "实际扣除的积分", example = "150")
    private Integer costPoints;

    @Schema(description = "O2O核销状态：0-待核销(未领货), 1-已领取(核销完毕)", example = "0")
    private Integer status;

    @Schema(description = "系统生成的提货核销验证码", example = "GIFT-6221-232")
    private String redeemCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "管理员线下扫码核销的时间")
    private LocalDateTime exchangeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "线上发起兑换的时间")
    private LocalDateTime createTime;

    // ================= 以下为非数据库字段，用于返回给前端渲染 =================

    @TableField(exist = false)
    @Schema(description = "兑换人姓名(联表展示用)")
    private String userName;

    @TableField(exist = false)
    @Schema(description = "商品名称(联表展示用)")
    private String goodsName;

    @TableField(exist = false)
    @Schema(description = "商品图片URL(联表展示用)")
    private String goodsImage;
}