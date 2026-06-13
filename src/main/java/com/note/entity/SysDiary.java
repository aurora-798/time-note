package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_diary")
@Schema(description = "日记内容")
public class SysDiary {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日记ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "日记日期")
    private LocalDate diaryDate;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "AI生成的日记内容")
    private String content;

    @Schema(description = "原始素材内容")
    private String sourceContent;

    @Schema(description = "日记风格")
    private String style;

    @Schema(description = "字数")
    private Integer wordCount;

    @Schema(description = "是否已编辑：0-否，1-是")
    private Integer isEdit;

    @Schema(description = "状态：0-草稿，1-已发布")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除标记")
    private Integer deleteFlag;
}
