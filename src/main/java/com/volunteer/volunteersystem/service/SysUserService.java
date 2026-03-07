package com.volunteer.volunteersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.volunteer.volunteersystem.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser login(String username, String password);
    void register(SysUser user);
}