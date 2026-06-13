package com.note.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.entity.SysLoginLog;
import com.note.mapper.SysLoginLogMapper;
import com.note.service.SysLoginLogService;
import org.springframework.stereotype.Service;

@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements SysLoginLogService {
}
