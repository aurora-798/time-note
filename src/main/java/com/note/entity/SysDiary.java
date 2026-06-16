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

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日记ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "日记日期")
    private LocalDate diaryDate;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "日记内容")
    private String content;


    @Schema(description = "字数")
    private Integer wordCount;

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
