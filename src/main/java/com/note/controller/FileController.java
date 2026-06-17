package com.note.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.note.common.Result;
import com.note.entity.SysMedia;
import com.note.service.SysMediaService;
import com.note.utils.QiniuUtils;
import com.note.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final QiniuUtils qiniuUtils;
    private final SysMediaService sysMediaService;

    /** mediaType -> 七牛云存储文件夹 */
    private static final Map<Integer, String> TYPE_FOLDER = Map.of(
            1, "image",
            2, "video",
            3, "avatar",
            4, "user-cover");

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<SysMedia> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件类型：1-图片，2-视频，3-头像，4-用户封面") @RequestParam Integer mediaType,
            @Parameter(description = "关联日记本ID") @RequestParam(required = false) Long bookId,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {

        if (file == null || file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        String folder = TYPE_FOLDER.get(mediaType);
        if (folder == null) {
            return Result.fail("不支持的文件类型，mediaType 仅支持 1-图片、2-视频、3-头像、4-用户封面");
        }

        String originalName = file.getOriginalFilename();
        String suffix = FileUtil.extName(originalName);
        String storedName = IdUtil.fastSimpleUUID() + (StrUtil.isBlank(suffix) ? "" : "." + suffix);

        String url;
        try {
            url = qiniuUtils.uploadByBytes(file.getBytes(), folder, storedName);
        } catch (IOException e) {
            return Result.fail("文件读取失败：" + e.getMessage());
        }
        if (StrUtil.isBlank(url)) {
            return Result.fail("文件上传失败，请稍后重试");
        }

        SysMedia media = new SysMedia();
        media.setUserId(UserUtils.currentUserId());
        media.setBookId(bookId);
        media.setMediaType(mediaType);
        media.setFileName(originalName);
        media.setFileUrl(url);
        media.setFileSize(file.getSize());
        media.setSuffix(suffix);
        media.setRemark(remark);
        sysMediaService.save(media);

        return Result.ok(media);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public Result<?> delete(@Parameter(description = "多媒体记录ID") @PathVariable Long id) {
        SysMedia media = sysMediaService.getById(id);
        if (media == null) {
            return Result.fail("文件不存在");
        }
        String key = qiniuUtils.resolveKey(media.getFileUrl());
        if (StrUtil.isNotBlank(key)) {
            qiniuUtils.delete(key);
        }
        sysMediaService.removeById(id);
        return Result.ok();
    }
}
