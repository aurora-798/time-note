package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.AiTokenRecord;
import com.note.service.AiTokenRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Token记录")
@RestController
@RequestMapping("/api/token-record")
@RequiredArgsConstructor
public class AiTokenRecordController {

    private final AiTokenRecordService aiTokenRecordService;

    @Operation(summary = "分页查询Token记录")
    @GetMapping("/page")
    public Result<Page<AiTokenRecord>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户ID筛选") @RequestParam(required = false) Long userId) {
        Page<AiTokenRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiTokenRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AiTokenRecord::getUserId, userId);
        }
        wrapper.orderByDesc(AiTokenRecord::getCallTime);
        return Result.ok(aiTokenRecordService.page(page, wrapper));
    }

    @Operation(summary = "根据ID查询Token记录")
    @GetMapping("/{id}")
    public Result<AiTokenRecord> getById(@Parameter(description = "记录ID") @PathVariable Long id) {
        return Result.ok(aiTokenRecordService.getById(id));
    }

    @Operation(summary = "新增Token记录")
    @PostMapping
    public Result<?> save(@RequestBody AiTokenRecord record) {
        aiTokenRecordService.save(record);
        return Result.ok();
    }

    @Operation(summary = "更新Token记录")
    @PutMapping
    public Result<?> update(@RequestBody AiTokenRecord record) {
        aiTokenRecordService.updateById(record);
        return Result.ok();
    }

    @Operation(summary = "删除Token记录")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "记录ID") @PathVariable Long id) {
        aiTokenRecordService.removeById(id);
        return Result.ok();
    }
}
