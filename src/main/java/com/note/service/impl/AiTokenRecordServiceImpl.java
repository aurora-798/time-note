package com.note.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.entity.AiTokenRecord;
import com.note.mapper.AiTokenRecordMapper;
import com.note.service.AiTokenRecordService;
import org.springframework.stereotype.Service;

@Service
public class AiTokenRecordServiceImpl extends ServiceImpl<AiTokenRecordMapper, AiTokenRecord> implements AiTokenRecordService {
}
