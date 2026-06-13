package com.note.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.common.Result;
import com.note.entity.SysUserVip;
import com.note.service.SysUserVipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "VIP权益管理")
@RestController
@RequestMapping("/api/user-vip")
@RequiredArgsConstructor
public class SysUserVipController {

    private final SysUserVipService sysUserVipService;

    @Operation(summary = "分页查询VIP记录")
    @GetMapping("/page")
    public Result<Page<SysUserVip>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysUserVip> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUserVip> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysUserVip::getCreateTime);
        return Result.ok(sysUserVipService.page(page, wrapper));
    }

    @Operation(summary = "根据ID查询VIP记录")
    @GetMapping("/{id}")
    public Result<SysUserVip> getById(@Parameter(description = "VIP记录ID") @PathVariable Long id) {
        return Result.ok(sysUserVipService.getById(id));
    }

    @Operation(summary = "新增VIP记录")
    @PostMapping
    public Result<?> save(@RequestBody SysUserVip sysUserVip) {
        sysUserVipService.save(sysUserVip);
        return Result.ok();
    }

    @Operation(summary = "更新VIP记录")
    @PutMapping
    public Result<?> update(@RequestBody SysUserVip sysUserVip) {
        sysUserVipService.updateById(sysUserVip);
        return Result.ok();
    }

    @Operation(summary = "删除VIP记录")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "VIP记录ID") @PathVariable Long id) {
        sysUserVipService.removeById(id);
        return Result.ok();
    }
}
