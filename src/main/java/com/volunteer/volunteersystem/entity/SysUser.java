package com.volunteer.volunteersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private Integer points;     // 积分
    private Integer status;     // 状态
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


}