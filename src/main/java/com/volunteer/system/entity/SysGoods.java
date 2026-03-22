package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_goods")
@Schema(description = "积分商城兑换商品实体类", name = "SysGoods")
public class SysGoods {
    @TableId(type = IdType.AUTO)
    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称", example = "定制环保帆布袋")
    private String name;

    @Schema(description = "商品详细描述")
    private String description;

    @Schema(description = "兑换该商品需消耗的可用积分", example = "150")
    private Integer pointsRequired;

    @Schema(description = "当前剩余库存(高并发下使用悲观锁控制)", example = "50")
    private Integer stock;

    @Schema(description = "商品主图URL")
    private String image;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上架时间")
    private LocalDateTime createTime;

    @Schema(description = "商品分类", example = "生活用品")
private String category;
}