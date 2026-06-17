package com.note.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysDiary;
import com.note.entity.request.diary.SysDiaryCreateRequest;
import com.note.entity.request.diary.SysDiaryDeleteRequest;
import com.note.entity.request.diary.SysDiaryEditRequest;
import com.note.entity.request.diary.SysDiaryPageRequest;
import com.note.entity.vo.diary.SysDiaryPageVo;
import com.note.service.SysDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日记管理")
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class SysDiaryController {

    private final SysDiaryService sysDiaryService;

    @Operation(summary = "分页查询日记")
    @PostMapping("/page")
    public Result<SysDiaryPageVo> getDiaryPage(SysDiaryPageRequest request) {
        Page<SysDiary> page = sysDiaryService.pageByUserId(request);
        SysDiaryPageVo vo = SysDiaryPageVo.builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .pages(page.getPages())
                .current(page.getCurrent())
                .size(page.getSize())
                .build();
        return Result.ok(vo);
    }

    @Operation(summary = "新增日记")
    @PostMapping("/create")
    public Result<?> createDiary(SysDiaryCreateRequest request) {
        boolean result = sysDiaryService.saveDiary(request);
        if (!result) {
            return Result.fail("创建失败");
        }
        return Result.ok();
    }

    @Operation(summary = "编辑日记")
    @PostMapping("/edit")
    public Result<?> editDiary(SysDiaryEditRequest request) {
        boolean result = sysDiaryService.updateDiary(request);
        if (!result) {
            return Result.fail("更新失败");
        }
        return Result.ok();
    }

    @Operation(summary = "删除日记")
    @PostMapping("/delete")
    public Result<?> deleteDiary(SysDiaryDeleteRequest request) {
        boolean result = sysDiaryService.deleteDiary(request);
        if (!result) {
            return Result.fail("删除失败");
        }
        return Result.ok();
    }
}
