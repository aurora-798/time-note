package com.note.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.entity.SysDiary;
import com.note.mapper.SysDiaryMapper;
import com.note.service.SysDiaryService;
import org.springframework.stereotype.Service;

@Service
public class SysDiaryServiceImpl extends ServiceImpl<SysDiaryMapper, SysDiary> implements SysDiaryService {
}
