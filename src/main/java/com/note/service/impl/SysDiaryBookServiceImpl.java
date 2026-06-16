package com.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.constant.ResultCode;
import com.note.entity.SysDiaryBook;
import com.note.entity.SysDiaryBookSecret;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.request.diarybook.SysDiaryBookDelRequest;
import com.note.entity.request.diarybook.SysDiaryBookEditRequest;
import com.note.entity.request.diarybook.SysDiaryBookVerifyRequest;
import com.note.exception.BusinessException;
import com.note.mapper.SysDiaryBookMapper;
import com.note.mapper.SysDiaryBookSecretMapper;
import com.note.service.SysDiaryBookService;
import com.note.utils.BcryptUtils;
import com.note.utils.UserUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.note.constant.SystemParamSettingConstant.DiaryBook_Max_Count;

@Service
public class SysDiaryBookServiceImpl extends ServiceImpl<SysDiaryBookMapper, SysDiaryBook> implements SysDiaryBookService {

    @Resource
    private SysDiaryBookMapper sysDiaryBookMapper;

    @Resource
    private SysDiaryBookSecretMapper sysDiaryBookSecretMapper;


    /**
     * 根据用户 ID 查询日记本列表
     * @return 返回日记本列表
     */
    @Override
    public List<SysDiaryBook> listByUserId() {
        // 获取用户 ID
        Long userId = UserUtils.currentUserId();
        if(userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        LambdaQueryWrapper<SysDiaryBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDiaryBook::getUserId, userId);
        return sysDiaryBookMapper.selectList(queryWrapper);
    }

    /**
     * 保存日记本
     * @param sysDiaryBookRequest 日记本信息
     * @return 是否保存成功
     */
    @Override
    @Transactional
    public boolean saveDiaryBook(SysDiaryBookCreateRequest sysDiaryBookRequest) {
        Integer encrypted = sysDiaryBookRequest.getEncrypted();
        SysDiaryBook sysDiaryBook = new SysDiaryBook();
        BeanUtil.copyProperties(sysDiaryBookRequest, sysDiaryBook);
        if(encrypted == 1) {
            sysDiaryBook.setEncrypted(1);
            // 加密：明文 → 哈希入库
            String hash = BcryptUtils.enBcrypt();
            SysDiaryBookSecret bookSecret = SysDiaryBookSecret.builder()
                    .bookId(sysDiaryBook.getId())
                    .secretHash(hash)
                    .build();
            sysDiaryBookSecretMapper.insert(bookSecret);
        }
        int res = sysDiaryBookMapper.insert(sysDiaryBook);
        return res > 0;
    }

    /**
     * 验证日记本
     * @param sysDiaryBookVerify 日记本信息
     */
    @Override
    public void DiaryBookVerify(SysDiaryBookVerifyRequest sysDiaryBookVerify) {
        Long bookId = sysDiaryBookVerify.getBookId();
        String password = sysDiaryBookVerify.getPassword();
        if(bookId == null || StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        LambdaQueryWrapper<SysDiaryBookSecret> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysDiaryBookSecret::getBookId, bookId);
        SysDiaryBookSecret sysDiaryBookSecret = sysDiaryBookSecretMapper.selectOne(lambdaQueryWrapper);
        if(sysDiaryBookSecret == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String hash = sysDiaryBookSecret.getSecretHash();
        boolean verify = BcryptUtils.checkBcrypt(password, hash);
        if(!verify) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
    }



    /**
     * 修改日记本
     * @param sysDiaryBookEditRequest 日记本信息
     * @return 是否修改成功
     */
    @Override
    @Transactional
    public boolean updateDiaryBook(SysDiaryBookEditRequest sysDiaryBookEditRequest) {
        String bookId = sysDiaryBookEditRequest.getBookId();
        String name = sysDiaryBookEditRequest.getName();
        if(StrUtil.isBlank(bookId) || StrUtil.isBlank(name)) {
            throw new BusinessException(ResultCode.Empty);
        }
        if(name.length() > DiaryBook_Max_Count) {
            throw new BusinessException(ResultCode.MORE_THAN_MAX_LENGTH);
        }
        SysDiaryBook sysDiaryBook = sysDiaryBookMapper.selectById(bookId);
        if(sysDiaryBook == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        sysDiaryBook.setName(name);
        sysDiaryBookMapper.updateById(sysDiaryBook);
        return true;
    }

    /**
     * 删除日记本
     * @param sysDiaryBookDelRequest 日记本信息
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteDiaryBook(SysDiaryBookDelRequest sysDiaryBookDelRequest) {
        String bookId = sysDiaryBookDelRequest.getBookId();
        if(StrUtil.isBlank(bookId)) {
            throw new BusinessException(ResultCode.Empty);
        }
        // TODO 删除日记本内的日记
        // 删除日记本
        int result = sysDiaryBookMapper.deleteById(bookId);
        return result > 0;
    }
}
