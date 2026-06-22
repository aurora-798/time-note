package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysDiary;
import com.note.entity.request.diary.*;
import com.note.entity.request.diarybook.BookPasswordRequest;
import com.note.entity.request.diarybook.SysDiaryBookCreateRequest;
import com.note.entity.vo.diary.SysDiaryFindVo;
import com.note.entity.vo.diary.SysDiaryPageVo;
import com.note.entity.vo.diary.SysDiaryWeatherVo;
import com.note.service.SysDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "日记管理")
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class SysDiaryController {

    private final SysDiaryService sysDiaryService;

    @Operation(summary = "分页查询日记")
    @PostMapping("/page")
    public Result<SysDiaryPageVo> getDiaryPage(@RequestBody SysDiaryPageRequest request) {
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
    public Result<?> createDiary(@RequestBody SysDiaryCreateRequest request) {
        boolean result = sysDiaryService.saveDiary(request);
        if (!result) {
            return Result.fail("创建失败");
        }
        return Result.ok();
    }

    @Operation(summary = "查询日记")
    @PostMapping("/{bookId}/{diaryId}")
    public Result<SysDiaryFindVo> findDiary(@PathVariable Long bookId,
                                            @PathVariable Long diaryId,
                                            @RequestBody(required = false) BookPasswordRequest request) {
        String password = request != null ? request.getPassword() : null;
        SysDiaryFindVo sysDiaryFindVo = sysDiaryService.findDiary(bookId, diaryId, password);
        return Result.ok(sysDiaryFindVo);
    }



    @Operation(summary = "编辑日记")
    @PostMapping("/edit")
    public Result<?> editDiary(@RequestBody SysDiaryEditRequest request) {
        boolean result = sysDiaryService.updateDiary(request);
        if (!result) {
            return Result.fail("更新失败");
        }
        return Result.ok();
    }

    @Operation(summary = "删除日记")
    @PostMapping("/delete")
    public Result<?> deleteDiary(@RequestBody SysDiaryDeleteRequest request) {
        boolean result = sysDiaryService.deleteDiary(request);
        if (!result) {
            return Result.fail("删除失败");
        }
        return Result.ok();
    }

    @Operation(summary = "根据经纬度获取气象数据")
    @PostMapping("/weather")
    public Result<SysDiaryWeatherVo> getWeather(@RequestBody SysDiaryWeatherRequest sysDiaryWeatherRequest) {
        return Result.ok(sysDiaryService.getWeatherData(sysDiaryWeatherRequest));
    }
}
