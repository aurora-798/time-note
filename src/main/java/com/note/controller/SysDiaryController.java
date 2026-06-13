package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysDiary;
import com.note.service.SysDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @GetMapping("/page")
    public Result<Page<SysDiary>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户ID筛选") @RequestParam(required = false) Long userId) {
        Page<SysDiary> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDiary> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysDiary::getUserId, userId);
        }
        wrapper.orderByDesc(SysDiary::getDiaryDate);
        return Result.ok(sysDiaryService.page(page, wrapper));
    }

    @Operation(summary = "根据ID查询日记")
    @GetMapping("/{id}")
    public Result<SysDiary> getById(@Parameter(description = "日记ID") @PathVariable Long id) {
        return Result.ok(sysDiaryService.getById(id));
    }

    @Operation(summary = "新增日记")
    @PostMapping
    public Result<?> save(@RequestBody SysDiary sysDiary) {
        sysDiaryService.save(sysDiary);
        return Result.ok();
    }

    @Operation(summary = "更新日记")
    @PutMapping
    public Result<?> update(@RequestBody SysDiary sysDiary) {
        sysDiaryService.updateById(sysDiary);
        return Result.ok();
    }

    @Operation(summary = "删除日记")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "日记ID") @PathVariable Long id) {
        sysDiaryService.removeById(id);
        return Result.ok();
    }
}
