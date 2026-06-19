package com.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.ai.model.CityData;
import com.note.ai.utils.RagUtils;
import com.note.constant.ResultCode;
import com.note.entity.SysDiary;
import com.note.entity.request.diary.*;
import com.note.entity.vo.diary.SysDiaryFindVo;
import com.note.entity.vo.diary.SysDiaryWeatherVo;
import com.note.exception.BusinessException;
import com.note.mapper.SysDiaryMapper;
import com.note.service.SysDiaryService;
import com.note.service.WeatherService;
import com.note.utils.UserUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import static com.note.constant.SystemParamSettingConstant.Diary_Title_Max_Count;

@Service
public class SysDiaryServiceImpl extends ServiceImpl<SysDiaryMapper, SysDiary> implements SysDiaryService {

    @Resource
    private WeatherService weatherService;

    @Resource
    private RagUtils ragUtils;

    /**
     * 新增日记同步向量库
     */
    public void saveDiaryToVector(SysDiary sysDiary) {
        // 1. 实体转带元数据文档
        Document document = ragUtils.toDocument(sysDiary);
        // 2. 自适应分块
        List<TextSegment> textSegments = ragUtils.autoSplit(document);
        // 3. 将分块批量写入向量库
        ragUtils.embeddingSaveTextAndStore(textSegments);
    }

    /**
     * 更新日记向量：先删旧，再存新
     */
    public void updateDiaryVector(SysDiary sysDiary) {
        String diaryIdStr = sysDiary.getId().toString();
        // 根据 diaryId 删除全部旧向量片段
        Filter filter = new IsEqualTo("diaryId", diaryIdStr);
        // 写入更新后的向量
        ragUtils.embeddingUpdateTextAndStore(filter,sysDiary);
    }

    /**
     * 删除日记对应向量
     */
    public void deleteDiaryVector(Long diaryId) {
        ragUtils.embeddingDelText(diaryId.toString());
    }

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

        // 保存 DS
        SysDiary sysDiary = new SysDiary();
        BeanUtil.copyProperties(request, sysDiary);
        sysDiary.setUserId(userId);
        sysDiary.setDiaryDate(LocalDate.now());
        sysDiary.setWordCount(request.getContent().length());
        boolean save = save(sysDiary);

        // 将日记添加到向量数据库
        try {
            saveDiaryToVector(sysDiary);
        } catch (Exception e) {
            log.error("日记向量入库失败，diaryId:" + sysDiary.getId());
        }
        return save;
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
        // 更新字数
        sysDiary.setWordCount(request.getContent().length());
        boolean update = updateById(sysDiary);

        if(update) {
            try {
                updateDiaryVector(sysDiary);
            } catch (Exception e) {
                log.error("日记向量更新失败，diaryId:" + request.getId());
            }

        }
        return update;
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
        boolean remove = removeById(request.getId());
        if(remove) {
            try {
                deleteDiaryVector(request.getId());
            } catch (Exception e) {
                log.error("日记向量删除失败，diaryId:" + request.getId());
            }
        }
        return remove;
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


    /**
     * 获取天气信息
     * @param sysDiaryWeatherRequest 天气信息参数
     * @return 天气信息
     */
    @Override
    public SysDiaryWeatherVo getWeatherData(SysDiaryWeatherRequest sysDiaryWeatherRequest) {
        Double lat = sysDiaryWeatherRequest.getLat();
        Double lng = sysDiaryWeatherRequest.getLng();
        if (lat == null || lng == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        SysDiaryWeatherVo sysDiaryWeatherVo = new SysDiaryWeatherVo();
        CityData weatherNow = weatherService.getWeatherNow(lng, lat);
        BeanUtil.copyProperties(weatherNow, sysDiaryWeatherVo);
        return sysDiaryWeatherVo;
    }
}
