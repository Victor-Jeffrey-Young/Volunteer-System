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
@TableName("sys_wish")
@Schema(description = "邻里微心愿实体类", name = "SysWish")
public class SysWish {
    @TableId(type = IdType.AUTO)
    @Schema(description = "心愿ID主键")
    private Long wishId;

    @Schema(description = "发起心愿的居民ID", example = "5")
    private Long requesterId;

    @Schema(description = "心愿简要标题", example = "帮孤寡老人代买生活用品")
    private String title;

    @Schema(description = "心愿分类：1-代办服务, 2-物资筹集, 3-咨询求助", example = "1")
    private String category;

    @Schema(description = "心愿详细内容描述")
    private String content;

    @Schema(description = "服务执行的具体地址")
    private String address;

    @Schema(description = "心愿达成后奖励志愿者的积分", example = "20")
    private Integer rewardPoints;

    @Schema(description = "心愿达成后核发的志愿工时", example = "1.5")
    private Double rewardHours;

    @Schema(description = "业务流转状态：0-待审核, 1-展示中(待认领), 2-办理中, 3-已达成, 4-已驳回, 5-已标记完成(待确认), 6-待结算", example = "1")
    private Integer status;
    
    @Schema(description = "管理员审核意见或处理备注")
    private String remarks;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "心愿发布申请时间")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "心愿最终达成完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "认领并执行该心愿的志愿者ID")
    private Long volunteerId;

    @TableField(exist = false)
    @Schema(description = "发起人真实姓名(联表展示用)")
    private String requesterName;

    @TableField(exist = false)
    @Schema(description = "发起人联系方式(联表展示用)")
    private String requesterPhone;

    @TableField(exist = false)
    @Schema(description = "志愿者真实姓名(联表展示用)")
    private String volunteerName;

    @TableField(exist = false)
    @Schema(description = "志愿者联系方式(联表展示用)")
    private String volunteerPhone;

    @TableField(exist = false)
    @Schema(description = "发起人头像(联表展示用)")
    private String requesterAvatar;

    @TableField(exist = false)
    @Schema(description = "志愿者头像(联表展示用)")
    private String volunteerAvatar;

    @Schema(description = "是否获得点赞评价：0-无, 1-已获赞", example = "1")
    @TableField("is_liked")
    private Integer isLiked = 0;
}
