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
import com.note.entity.vo.diary.SysDiaryFindVo;
import com.note.exception.BusinessException;
import com.note.mapper.SysDiaryMapper;
import com.note.service.SysDiaryService;
import com.note.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
        if (request.getBookId() != null) {
            wrapper.eq(SysDiary::getBookId, request.getBookId());
        }
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
        sysDiary.setDiaryDate(LocalDate.now());
        return save(sysDiary);
    }

    @Override
    @Transactional
    public boolean updateDiary(SysDiaryEditRequest request) {
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        if (StrUtil.isNotBlank(request.getTitle()) && request.getTitle().length() > Diary_Title_Max_Count) {
            throw new BusinessException(ResultCode.MORE_THAN_MAX_LENGTH);
        }
        SysDiary existing = getById(request.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SysDiary sysDiary = new SysDiary();
        BeanUtil.copyProperties(request, sysDiary);
        return updateById(sysDiary);
    }

    @Override
    @Transactional
    public boolean deleteDiary(SysDiaryDeleteRequest request) {
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        SysDiary existing = getById(request.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return removeById(request.getId());
    }


    /**
     * 查询日记
     * @param bookId 日记本 ID
     * @param diaryId 日记 ID
     * @return 日记信息
     */
    @Override
    public SysDiaryFindVo findDiary(Long bookId, Long diaryId) {
        if(bookId == null || diaryId == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        Long userId = UserUtils.currentUserId();
        if(userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        LambdaQueryWrapper<SysDiary> sysDiaryLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysDiaryLambdaQueryWrapper.eq(SysDiary::getId, diaryId)
                .eq(SysDiary::getUserId, userId);
        SysDiary sysDiary = baseMapper.selectOne(sysDiaryLambdaQueryWrapper);
        Long diaryUserId = sysDiary.getUserId();
        if(!diaryUserId.equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SysDiaryFindVo sysDiaryFindVo = new SysDiaryFindVo();
        BeanUtil.copyProperties(sysDiary, sysDiaryFindVo);
        return sysDiaryFindVo;
    }
}
