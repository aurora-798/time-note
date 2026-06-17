package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_media")
@Schema(description = "多媒体文件")
public class SysMedia {

    @TableId(type = IdType.AUTO)
    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "关联日记本 ID")
    private Long bookId;

    @Schema(description = "文件类型")
    private Integer mediaType;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件 URL")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件后缀")
    private String suffix;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除标记")
    private Integer deleteFlag;
}
