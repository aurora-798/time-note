package com.note.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.entity.SysUser;
import com.note.mapper.SysUserMapper;
import com.note.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}
