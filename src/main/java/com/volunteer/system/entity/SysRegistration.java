package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_registration")
@Schema(description = "志愿者报名与打卡流转记录", name = "SysRegistration")
public class SysRegistration {
    @TableId(type = IdType.AUTO)
    @Schema(description = "报名记录ID")
    private Long regId;

    @Schema(description = "关联的志愿者用户ID")
    private Long userId;

    @Schema(description = "关联的活动ID")
    private Long activityId;

    @Schema(description = "业务流转状态：0-待审, 1-通过(待签到), 2-已拒绝, 3-已完结(已发工时), 4-已取消, 5-已签到(进行中), 6-已签退(待结算)", example = "0")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "提交报名的时间")
    private LocalDateTime applyTime;

    // 🚨 补上刚才漏掉的审核时间字段
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "管理员审核时间")
    private LocalDateTime auditTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扫码签到时间")
    private LocalDateTime signInTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扫码签退时间")
    private LocalDateTime signOutTime;

    @Schema(description = "最终核发工时(结算时填入)", example = "2.5")
    private BigDecimal actualHours;

    @Schema(description = "本次服务实际获得的积分", example = "25")
    private Integer rewardPoints;

    @Schema(description = "审核反馈或拒绝理由")
    private String remarks;

    // ================= 以下为非数据库字段，用于返回给前端渲染 =================

    @TableField(exist = false)
    @Schema(description = "志愿者真实姓名(联表展示用)")
    private String realName;

    @TableField(exist = false)
    @Schema(description = "活动标题(联表展示用)")
    private String activityTitle;

    @TableField(exist = false)
    @Schema(description = "活动地点(联表展示用)")
    private String activityLocation;

    @TableField(exist = false)
    @Schema(description = "当前活动的主状态(用于判断能否唤起扫码打卡)", example = "1")
    private Integer activityStatus;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动预定开始时间(联表展示用)")
    private LocalDateTime activityStartTime;
}