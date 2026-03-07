package com.volunteer.volunteersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long userId;        // 用户ID
    private String username;    // 登录账号
    private String password;    // 登录密码
    private String realName;    // 真实姓名
    private Integer gender;     // 性别
    private String phone;       // 手机号
    private String avatar;      // 头像URL
    private String email;       // 邮箱
    private String role;        // 角色 (ADMIN/VOLUNTEER)
    private BigDecimal totalHours; // 累计时长
    // 🚨 修复点：确保这里已经改名为 totalPoints，并使用 @TableField 注解显式映射数据库列名
    @TableField("total_points")
    private Integer totalPoints;

    // 🚨 修复点：确保这里叫 currentPoints，并使用 @TableField 注解显式映射数据库列名
    @TableField("current_points")
    private Integer currentPoints;
    private Integer status;     // 状态
    private String skills; // JSON字符串
    private String availableTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


}