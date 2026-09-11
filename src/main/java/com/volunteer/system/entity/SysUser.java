package com.volunteer.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "用户信息实体类", name = "SysUser")
public class SysUser {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID主键", example = "1")
    private Long userId;

    @Schema(description = "登录账号(唯一)", example = "admin")
    private String username;

    /**
     * 登录密码。
     *
     * WRITE_ONLY：允许从请求体反序列化（注册/改密需要），但序列化响应时一律丢弃，
     * 防止 /api/user/page 这类直接返回实体的接口把 BCrypt 哈希泄漏给前端。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "登录密码(仅入参，永不回传)", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "真实姓名(用于荣誉证书生成)", example = "张三")
    private String realName;

    @Schema(description = "性别：1-男, 2-女, 0-未知", example = "1")
    private Integer gender;

    @Schema(description = "联系手机号码", example = "13800000000")
    private String phone;

    @Schema(description = "电子邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "系统角色：ADMIN-管理员, VOLUNTEER-志愿者, RESIDENT-居民", example = "VOLUNTEER")
    private String role;

    @Schema(description = "技能特长(JSON数组字符串)", example = "[\"医疗急救\", \"心理疏导\"]")
    private String skills;

    @Schema(description = "日常空闲时间段", example = "周末")
    private String availableTime;

    @Schema(description = "累计志愿服务总时长", example = "12.5")
    private BigDecimal totalHours;

    @TableField("total_points")
    @Schema(description = "累计荣誉总积分(决定志愿段位，只增不减)", example = "150")
    private Integer totalPoints;

    @TableField("current_points")
    @Schema(description = "可用消费积分余额(用于商城兑换)", example = "100")
    private Integer currentPoints;

    @Schema(description = "账号状态：1-正常, 0-已封禁", example = "1")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "账号注册时间")
    private LocalDateTime createTime;

    @Schema(description = "昨日积分排名")
    private Integer lastRank;

    @Schema(description = "昨日时长排名")
    private Integer lastHoursRank;

    @Schema(description = "获得累计点赞数")
    @TableField("likes")
    private Integer likes = 0;
}