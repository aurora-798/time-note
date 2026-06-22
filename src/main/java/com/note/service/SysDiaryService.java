package com.note.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.note.entity.SysDiary;
import com.note.entity.request.diary.*;
import com.note.entity.vo.diary.SysDiaryFindVo;
import com.note.entity.vo.diary.SysDiaryWeatherVo;

public interface SysDiaryService extends IService<SysDiary> {

    /**
     * 分页查询用户日记
     * @param request 分页参数
     * @return 分页结果
     */
    Page<SysDiary> pageByUserId(SysDiaryPageRequest request);

    /**
     * 保存日记
     * @param request 日记信息
     * @return 是否保存成功
     */
    boolean saveDiary(SysDiaryCreateRequest request);

    /**
     * 修改日记
     * @param request 日记信息
     * @return 是否修改成功
     */
    boolean updateDiary(SysDiaryEditRequest request);

    /**
     * 删除日记
     * @param request 日记 ID
     * @return 是否删除成功
     */
    boolean deleteDiary(SysDiaryDeleteRequest request);

    /**
     * 查询日记
     * @param bookId 日记本 ID
     * @param diaryId 日记 ID
     * @return 日记信息
     */
    SysDiaryFindVo findDiary(Long bookId, Long diaryId, String password);

    /**
     * 获取天气信息
     * @param sysDiaryWeatherRequest 天气信息参数
     * @return 天气信息
     */
    SysDiaryWeatherVo getWeatherData(SysDiaryWeatherRequest sysDiaryWeatherRequest);
}
