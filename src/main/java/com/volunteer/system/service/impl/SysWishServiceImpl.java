package com.volunteer.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.volunteer.system.entity.SysWish;
import com.volunteer.system.mapper.SysWishMapper;
import com.volunteer.system.service.SysWishService;
import org.springframework.stereotype.Service;

@Service
public class SysWishServiceImpl extends ServiceImpl<SysWishMapper, SysWish> implements SysWishService {
}
