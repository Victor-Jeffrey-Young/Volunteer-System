package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_activity")
public class SysActivity {
    @TableId(type = IdType.AUTO)
    private Long activityId;
    private String title;
    private String content;
    private String type;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer capacity;    // 招募人数
    private Integer currentNum;  // 已报名人数（默认0）
    private BigDecimal rewardHours; // 奖励时长
    private Integer status;      // 0-招募中, 1-进行中, 2-已结束, 3-已取消
    private Long organizerId;    // 发布人ID
    private LocalDateTime createTime;
}
