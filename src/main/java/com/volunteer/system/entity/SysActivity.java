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
@TableName("sys_activity")
@Schema(description = "志愿服务活动实体类", name = "SysActivity")
public class SysActivity {
    @TableId(type = IdType.AUTO)
    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "活动标题", example = "周末社区清扫活动")
    private String title;

    @Schema(description = "活动详细描述内容")
    private String content;

    @Schema(description = "活动类型（如：环境保护、社区服务）", example = "环境保护")
    private String type;

    @Schema(description = "活动地点", example = "阳光社区公园")
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "招募志愿者人数上限", example = "30")
    private Integer capacity;

    @Schema(description = "当前有效报名人数（冗余统计字段）", example = "15")
    private Integer currentNum;

    @Schema(description = "预计固定奖励工时", example = "3.0")
    private BigDecimal rewardHours;

    @Schema(description = "活动生命周期状态：0-招募中, 1-进行中(可打卡), 2-已结束, 3-已取消", example = "0")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "活动发布时间")
    private LocalDateTime createTime;

    @TableField("required_skills")
    @Schema(description = "专业技能需求", example = "[\"医疗急救\"]")
    private String requiredSkills;

}