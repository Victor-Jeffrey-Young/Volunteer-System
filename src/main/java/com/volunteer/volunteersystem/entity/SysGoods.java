package com.volunteer.volunteersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_goods")
public class SysGoods {
    @TableId(type = IdType.AUTO)
    private Long goodsId;
    private String name;
    private String description;
    private Integer pointsRequired;
    private Integer stock;
    private String image;
    private LocalDateTime createTime;
}