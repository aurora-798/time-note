package com.note.entity;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("diary_book")
@Schema(description = "日记本")
public class SysDiaryBook implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日记本ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "笔记本名称")
    private String name;

    @Schema(description = "封面地址")
    private String cover;

    @Schema(description = "所用字体")
    private String font;

    @Schema(description = "是否加密")
    private Integer encrypted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @Schema(description = "逻辑删除标记")
    private Integer deleteFlag;
}
