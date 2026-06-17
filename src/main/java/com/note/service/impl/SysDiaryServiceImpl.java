package com.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.constant.ResultCode;
import com.note.entity.SysDiary;
import com.note.entity.request.diary.SysDiaryCreateRequest;
import com.note.entity.request.diary.SysDiaryDeleteRequest;
import com.note.entity.request.diary.SysDiaryEditRequest;
import com.note.entity.request.diary.SysDiaryPageRequest;
import com.note.exception.BusinessException;
import com.note.mapper.SysDiaryMapper;
import com.note.service.SysDiaryService;
import com.note.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.note.constant.SystemParamSettingConstant.Diary_Title_Max_Count;

@Service
public class SysDiaryServiceImpl extends ServiceImpl<SysDiaryMapper, SysDiary> implements SysDiaryService {

    @Override
    public Page<SysDiary> pageByUserId(SysDiaryPageRequest request) {
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        Page<SysDiary> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDiary::getUserId, userId);
        wrapper.orderByDesc(SysDiary::getDiaryDate);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean saveDiary(SysDiaryCreateRequest request) {
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if (StrUtil.isBlank(request.getTitle())) {
            throw new BusinessException(ResultCode.Empty);
        }
        if (request.getTitle().length() > Diary_Title_Max_Count) {
            throw new BusinessException(ResultCode.MORE_THAN_MAX_LENGTH);
        }
        SysDiary sysDiary = new SysDiary();
        BeanUtil.copyProperties(request, sysDiary);
        sysDiary.setUserId(userId);
        return save(sysDiary);
    }

    @Override
    @Transactional
    public boolean updateDiary(SysDiaryEditRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        if (StrUtil.isNotBlank(request.getTitle()) && request.getTitle().length() > Diary_Title_Max_Count) {
            throw new BusinessException(ResultCode.MORE_THAN_MAX_LENGTH);
        }
        SysDiary sysDiary = new SysDiary();
        BeanUtil.copyProperties(request, sysDiary);
        return updateById(sysDiary);
    }

    @Override
    @Transactional
    public boolean deleteDiary(SysDiaryDeleteRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        return removeById(request.getId());
    }
}
