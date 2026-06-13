package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysLoginLog;
import com.note.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录日志")
@RestController
@RequestMapping("/api/login-log")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService sysLoginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    public Result<Page<SysLoginLog>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名模糊搜索") @RequestParam(required = false) String username) {
        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        wrapper.orderByDesc(SysLoginLog::getCreateTime);
        return Result.ok(sysLoginLogService.page(page, wrapper));
    }

    @Operation(summary = "根据ID查询登录日志")
    @GetMapping("/{id}")
    public Result<SysLoginLog> getById(@Parameter(description = "日志ID") @PathVariable Long id) {
        return Result.ok(sysLoginLogService.getById(id));
    }

    @Operation(summary = "新增登录日志")
    @PostMapping
    public Result<?> save(@RequestBody SysLoginLog loginLog) {
        sysLoginLogService.save(loginLog);
        return Result.ok();
    }

    @Operation(summary = "更新登录日志")
    @PutMapping
    public Result<?> update(@RequestBody SysLoginLog loginLog) {
        sysLoginLogService.updateById(loginLog);
        return Result.ok();
    }

    @Operation(summary = "删除登录日志")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "日志ID") @PathVariable Long id) {
        sysLoginLogService.removeById(id);
        return Result.ok();
    }
}
