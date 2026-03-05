package com.volunteer.volunteersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_registration")
public class SysRegistration {
    @TableId(type = IdType.AUTO)
    private Long regId;
    private Long userId;
    private Long activityId;
    private Integer status; // 0-待审核, 1-报名成功, 2-已拒绝, 3-已发放工时
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private BigDecimal actualHours;
    private String remarks;

    // 非数据库字段，用于前端连表展示
    @TableField(exist = false)
    private String activityTitle;

    @TableField(exist = false)
    private String activityLocation;
    // 在 SysRegistration.java 中追加一个字段

    @TableField(exist = false)
    private String realName; // 志愿者的真实姓名

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signInTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signOutTime;

    // 🚨 新增：用于前端判断是否可以签到
    @TableField(exist = false)
    private Integer activityStatus; // 活动状态

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime activityStartTime; // 活动开始时间
}