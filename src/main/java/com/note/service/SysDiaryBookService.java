package com.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.note.entity.SysDiaryBook;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.request.diarybook.SysDiaryBookDelRequest;
import com.note.entity.request.diarybook.SysDiaryBookEditRequest;
import com.note.entity.request.diarybook.SysDiaryBookVerifyRequest;
import com.note.entity.vo.diarybook.SysDiaryBookFindVo;

import java.util.List;

public interface SysDiaryBookService extends IService<SysDiaryBook> {

    /**
     * 根据用户 ID 查询日记本列表
     * @return 返回日记本列表
     */
    List<SysDiaryBook> listByUserId();


    /**
     * 保存日记本
     * @param sysDiaryBookRequest 日记本信息
     * @return 是否保存成功
     */
    boolean saveDiaryBook(SysDiaryBookCreateRequest sysDiaryBookRequest);

    /**
     * 验证日记本
     * @param sysDiaryBookVerify 日记本信息
     */
    void DiaryBookVerify(SysDiaryBookVerifyRequest sysDiaryBookVerify);

    /**
     * 修改日记本
     * @param sysDiaryBookEditRequest 日记本信息
     * @return 是否修改成功
     */
    boolean updateDiaryBook(SysDiaryBookEditRequest sysDiaryBookEditRequest);


    /**
     * 删除日记本
     * @param sysDiaryBookDelRequest 日记本信息
     * @return 是否删除成功
     */
    boolean deleteDiaryBook(SysDiaryBookDelRequest sysDiaryBookDelRequest);

    /**
     * 根据日记本 ID 查询日记本信息
     * @param bookId 日记本 ID
     * @return 日记本信息
     */
    SysDiaryBookFindVo listByBookId(Long bookId);
}
