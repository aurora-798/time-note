package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysMedia;
import com.note.service.SysMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "多媒体管理")
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class SysMediaController {

    private final SysMediaService sysMediaService;

    @Operation(summary = "分页查询多媒体")
    @GetMapping("/page")
    public Result<Page<SysMedia>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户ID筛选") @RequestParam(required = false) Long userId) {
        Page<SysMedia> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysMedia> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysMedia::getUserId, userId);
        }
        wrapper.orderByDesc(SysMedia::getCreateTime);
        return Result.ok(sysMediaService.page(page, wrapper));
    }

    @Operation(summary = "根据ID查询多媒体")
    @GetMapping("/{id}")
    public Result<SysMedia> getById(@Parameter(description = "多媒体ID") @PathVariable Long id) {
        return Result.ok(sysMediaService.getById(id));
    }

    @Operation(summary = "新增多媒体")
    @PostMapping
    public Result<?> save(@RequestBody SysMedia sysMedia) {
        sysMediaService.save(sysMedia);
        return Result.ok();
    }

    @Operation(summary = "更新多媒体")
    @PutMapping
    public Result<?> update(@RequestBody SysMedia sysMedia) {
        sysMediaService.updateById(sysMedia);
        return Result.ok();
    }

    @Operation(summary = "删除多媒体")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "多媒体ID") @PathVariable Long id) {
        sysMediaService.removeById(id);
        return Result.ok();
    }
}
