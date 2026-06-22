package com.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.constant.ResultCode;
import com.note.entity.SysDiary;
import com.note.entity.SysDiaryBook;
import com.note.entity.SysDiaryBookSecret;
import com.note.entity.SysMedia;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.request.diarybook.SysDiaryBookDelRequest;
import com.note.entity.request.diarybook.SysDiaryBookEditRequest;
import com.note.entity.request.diarybook.SysDiaryBookVerifyRequest;
import com.note.entity.vo.diarybook.SysDiaryBookFindVo;
import com.note.exception.BusinessException;
import com.note.mapper.SysDiaryBookMapper;
import com.note.mapper.SysDiaryBookSecretMapper;
import com.note.mapper.SysDiaryMapper;
import com.note.mapper.SysMediaMapper;
import com.note.service.SysDiaryBookService;
import com.note.utils.BcryptUtils;
import com.note.utils.QiniuUtils;
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
    private SysDiaryMapper sysDiaryMapper;

    @Resource
    private SysDiaryBookSecretMapper sysDiaryBookSecretMapper;

    @Resource
    private SysMediaMapper sysMediaMapper;

    @Resource
    private QiniuUtils qiniuUtils;


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
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        Integer encrypted = sysDiaryBookRequest.getEncrypted();
        SysDiaryBook sysDiaryBook = new SysDiaryBook();
        BeanUtil.copyProperties(sysDiaryBookRequest, sysDiaryBook);
        sysDiaryBook.setUserId(userId);
        if (encrypted == 1) {
            sysDiaryBook.setEncrypted(1);
        }
        int res = sysDiaryBookMapper.insert(sysDiaryBook);
        if (encrypted == 1 && res > 0) {
            String hash = BcryptUtils.enBcrypt(sysDiaryBookRequest.getPassword());
            SysDiaryBookSecret bookSecret = SysDiaryBookSecret.builder()
                    .bookId(sysDiaryBook.getId())
                    .secretHash(hash)
                    .build();
            sysDiaryBookSecretMapper.insert(bookSecret);
        }
        // 自定义封面（有上传元数据）才创建媒体记录；预设封面是共享 CDN 资源，不需要
        String cover = sysDiaryBookRequest.getCover();
        if (res > 0 && StrUtil.isNotBlank(cover) && StrUtil.isNotBlank(sysDiaryBookRequest.getCoverFileName())) {
            SysMedia media = new SysMedia();
            media.setUserId(userId);
            media.setBookId(sysDiaryBook.getId());
            media.setMediaType(1);
            media.setFileUrl(cover);
            media.setFileName(sysDiaryBookRequest.getCoverFileName());
            media.setFileSize(sysDiaryBookRequest.getCoverFileSize());
            media.setSuffix(sysDiaryBookRequest.getCoverSuffix());
            sysMediaMapper.insert(media);
        }
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
        if (bookId == null || StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        assertBookAccess(bookId, password);
    }

    /**
     * 校验当前用户对日记本的访问权限（归属权 + 加密密码）
     * 如果没有权限则抛出异常，有权限则返回日记本对象
     */
    @Override
    public SysDiaryBook assertBookAccess(Long bookId, String password) {
        if (bookId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        SysDiaryBook sysDiaryBook = sysDiaryBookMapper.selectById(bookId);
        if (sysDiaryBook == null || !sysDiaryBook.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (sysDiaryBook.getEncrypted() == null || sysDiaryBook.getEncrypted() != 1) {
            return sysDiaryBook;
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        LambdaQueryWrapper<SysDiaryBookSecret> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysDiaryBookSecret::getBookId, bookId);
        SysDiaryBookSecret sysDiaryBookSecret = sysDiaryBookSecretMapper.selectOne(lambdaQueryWrapper);
        if (sysDiaryBookSecret == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!BcryptUtils.checkBcrypt(password, sysDiaryBookSecret.getSecretHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        return sysDiaryBook;
    }



    /**
     * 修改日记本
     * @param sysDiaryBookEditRequest 日记本信息
     * @return 是否修改成功
     */
    @Override
    @Transactional
    public boolean updateDiaryBook(SysDiaryBookEditRequest sysDiaryBookEditRequest) {
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        Long bookId = sysDiaryBookEditRequest.getBookId();
        String name = sysDiaryBookEditRequest.getName();
        if (bookId == null || StrUtil.isBlank(name)) {
            throw new BusinessException(ResultCode.Empty);
        }
        if (name.length() > DiaryBook_Max_Count) {
            throw new BusinessException(ResultCode.MORE_THAN_MAX_LENGTH);
        }
        SysDiaryBook sysDiaryBook = sysDiaryBookMapper.selectById(bookId);
        if (sysDiaryBook == null || !sysDiaryBook.getUserId().equals(userId)) {
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
        Long userId = UserUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        Long bookId = sysDiaryBookDelRequest.getBookId();
        if (bookId == null) {
            throw new BusinessException(ResultCode.Empty);
        }
        SysDiaryBook existing = sysDiaryBookMapper.selectById(bookId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 删除关联的媒体文件（对象存储 + 数据库记录）
        LambdaQueryWrapper<SysMedia> mediaWrapper = new LambdaQueryWrapper<>();
        mediaWrapper.eq(SysMedia::getBookId, bookId);
        List<SysMedia> mediaList = sysMediaMapper.selectList(mediaWrapper);
        for (SysMedia media : mediaList) {
            String key = qiniuUtils.resolveKey(media.getFileUrl());
            if (StrUtil.isNotBlank(key)) {
                qiniuUtils.delete(key);
            }
        }
        if (!mediaList.isEmpty()) {
            sysMediaMapper.delete(mediaWrapper);
        }
        // 删除所有关联日记（逻辑删除）
        LambdaQueryWrapper<SysDiary> diaryWrapper = new LambdaQueryWrapper<>();
        diaryWrapper.eq(SysDiary::getBookId, bookId);
        sysDiaryMapper.delete(diaryWrapper);
        // 删除日记本
        int result = sysDiaryBookMapper.deleteById(bookId);
        return result > 0;
    }

    /**
     * 根据日记本 ID 查询日记本信息
     * @param bookId 日记本 ID
     * @return 日记本信息
     */
    @Override
    public SysDiaryBookFindVo listByBookId(Long bookId, String password) {
        if(bookId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        assertBookAccess(bookId, password);
        SysDiaryBook sysDiaryBook = sysDiaryBookMapper.selectById(bookId);
        SysDiaryBookFindVo sysDiaryBookFindVo = new SysDiaryBookFindVo();
        BeanUtil.copyProperties(sysDiaryBook, sysDiaryBookFindVo);
        return sysDiaryBookFindVo;
    }
}
