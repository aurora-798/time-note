package com.note.controller;


import com.note.common.Result;
import com.note.entity.SysDiaryBook;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.request.diarybook.SysDiaryBookDelRequest;
import com.note.entity.request.diarybook.SysDiaryBookEditRequest;
import com.note.entity.request.diarybook.SysDiaryBookVerifyRequest;
import com.note.entity.vo.diarybook.SysDiaryBookCreateVo;
import com.note.entity.vo.diarybook.SysDiaryBookDelVo;
import com.note.entity.vo.diarybook.SysDiaryBookEditVo;
import com.note.entity.vo.diarybook.SysDiaryBookListVo;
import com.note.service.SysDiaryBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "日记本管理")
@RestController
@RequestMapping(("/api/diarybook"))
@RequiredArgsConstructor
public class SysDiaryBookController {

    private final SysDiaryBookService sysDiaryBookService;

    @Operation(summary = "获取用户日记本列表")
    @PostMapping("/list")
    public Result<SysDiaryBookListVo> getDiaryBookList() {
        List<SysDiaryBook> sysDiaryBooks = sysDiaryBookService.listByUserId();

        SysDiaryBookListVo sysDiaryBookVo = SysDiaryBookListVo.builder()
                .list(sysDiaryBooks)
                .entryCount(sysDiaryBooks.size())
                .build();

        return Result.ok(sysDiaryBookVo);
    }

    @Operation(summary = "创建日记本")
    @PostMapping("/create")
    public Result<SysDiaryBookCreateVo> createDiaryBook(SysDiaryBookCreateRequest sysDiaryBookRequest) {
        boolean save = sysDiaryBookService.saveDiaryBook(sysDiaryBookRequest);
        if (!save) {
            return Result.fail("创建失败");
        }
        return Result.ok();
    }

    @Operation(summary = "验证日记本密码")
    @PostMapping("/verify")
    public Result<SysDiaryBookVerifyRequest> verifyDiaryBook(SysDiaryBookVerifyRequest sysDiaryBookVerify) {
        sysDiaryBookService.DiaryBookVerify(sysDiaryBookVerify);
        return Result.ok();
    }

    @Operation(summary = "编辑笔记本")
    @PostMapping("/edit")
    public Result<SysDiaryBookEditVo> editDiaryBook(SysDiaryBookEditRequest sysDiaryBookEditRequest) {
        boolean result = sysDiaryBookService.updateDiaryBook(sysDiaryBookEditRequest);
        if (!result) {
            return Result.fail("更新失败");
        }
        return Result.ok();
    }


    @Operation(summary = "删除日记本")
    @PostMapping("/delete")
    public Result<SysDiaryBookDelVo> deleteDiaryBook(SysDiaryBookDelRequest sysDiaryBookDelRequest) {
        boolean result = sysDiaryBookService.deleteDiaryBook(sysDiaryBookDelRequest);
        if (!result) {
            return Result.fail("删除失败");
        }
        return Result.ok();
    }
}
