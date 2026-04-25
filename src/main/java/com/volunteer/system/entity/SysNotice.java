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
@TableName("sys_notice")
@Schema(description = "社区新闻与官方公告信息表", name = "SysNotice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    @Schema(description = "公告ID")
    private Long noticeId;

    @Schema(description = "公告大标题", example = "关于夏季社区防汛志愿服务的招募")
    private String title;

    @Schema(description = "公告详细正文")
    private String content;

    @Schema(description = "内容分类：1-重要通知(红色标签), 2-志愿新闻(绿色标签)", example = "1")
    private Integer type;

    @Schema(description = "发布人(管理员)的用户ID")
    private Long publisherId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "公告发布时间")
    private LocalDateTime createTime;

    @TableField(exist = false)
    @Schema(description = "发布人姓名(联表展示用)")
    private String publisherName;

    @TableField(exist = false) // 不是数据库表里的字段，防止报错
    private Integer isRead;

}