package com.volunteer.volunteersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.volunteer.volunteersystem.entity.SysUser; // 检查导入是否正确
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> { // 🚨 必须继承 BaseMapper
}