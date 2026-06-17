package com.note.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.Result;
import com.note.entity.SysDiary;
import com.note.entity.SysDiaryBook;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.request.diarybook.SysDiaryBookDelRequest;
import com.note.entity.request.diarybook.SysDiaryBookEditRequest;
import com.note.entity.request.diarybook.SysDiaryBookVerifyRequest;
import com.note.entity.vo.diarybook.*;
import com.note.service.SysDiaryBookService;
import com.note.service.SysDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "日记本管理")
@RestController
@RequestMapping(("/api/diarybook"))
@RequiredArgsConstructor
public class SysDiaryBookController {

    private final SysDiaryBookService sysDiaryBookService;

    private final SysDiaryService sysDiaryService;

    @Operation(summary = "获取用户日记本列表")
    @PostMapping("/list")
    public Result<List<SysDiaryBookListVo>> getDiaryBookList() {
        List<SysDiaryBook> sysDiaryBooks = sysDiaryBookService.listByUserId();

        List<SysDiaryBookListVo> diaryBookListVos = sysDiaryBooks.stream()
                .map(sysDiaryBook -> {
                    Long bookId = sysDiaryBook.getId();
                    LambdaQueryWrapper<SysDiary> sysDiaryLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    sysDiaryLambdaQueryWrapper.eq(SysDiary::getBookId, bookId);
                    long entryCount = sysDiaryService.count(sysDiaryLambdaQueryWrapper);
                    SysDiaryBookListVo sysDiaryBookListVo = new SysDiaryBookListVo();
                    SysDiaryBookFindVo sysDiaryBookFindVo = new SysDiaryBookFindVo();
                    BeanUtil.copyProperties(sysDiaryBook,sysDiaryBookFindVo);

                    sysDiaryBookListVo.setSysDiaryBookFindVo(sysDiaryBookFindVo);
                    sysDiaryBookListVo.setEntryCount(entryCount);
                    return sysDiaryBookListVo;
                }).toList();

        return Result.ok(diaryBookListVos);
    }

    @Operation(summary = "根据 bookId 获取用户日记本信息")
    @PostMapping("/{bookId}")
    public Result<SysDiaryBookFindVo> getDiaryBookById(@PathVariable Long bookId) {
        SysDiaryBookFindVo sysDiaryBook = sysDiaryBookService.listByBookId(bookId);
        return Result.ok(sysDiaryBook);
    }



    @Operation(summary = "创建日记本")
    @PostMapping("/create")
    public Result<SysDiaryBookCreateVo> createDiaryBook(@RequestBody SysDiaryBookCreateRequest sysDiaryBookRequest) {
        boolean save = sysDiaryBookService.saveDiaryBook(sysDiaryBookRequest);
        if (!save) {
            return Result.fail("创建失败");
        }
        return Result.ok();
    }

    @Operation(summary = "验证日记本密码")
    @PostMapping("/verify")
    public Result<?> verifyDiaryBook(@RequestBody SysDiaryBookVerifyRequest sysDiaryBookVerify) {
        sysDiaryBookService.DiaryBookVerify(sysDiaryBookVerify);
        return Result.ok();
    }

    @Operation(summary = "编辑笔记本")
    @PostMapping("/edit")
    public Result<SysDiaryBookEditVo> editDiaryBook(@RequestBody SysDiaryBookEditRequest sysDiaryBookEditRequest) {
        boolean result = sysDiaryBookService.updateDiaryBook(sysDiaryBookEditRequest);
        if (!result) {
            return Result.fail("更新失败");
        }
        return Result.ok();
    }


    @Operation(summary = "删除日记本")
    @PostMapping("/delete")
    public Result<SysDiaryBookDelVo> deleteDiaryBook(@RequestBody SysDiaryBookDelRequest sysDiaryBookDelRequest) {
        boolean result = sysDiaryBookService.deleteDiaryBook(sysDiaryBookDelRequest);
        if (!result) {
            return Result.fail("删除失败");
        }
        return Result.ok();
    }
}
