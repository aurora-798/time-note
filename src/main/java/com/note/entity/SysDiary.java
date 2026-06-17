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
    @Schema(description = "日记 ID")
    private Long id;

    @Schema(description = "日记本 ID")
    private Long bookId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "日记日期")
    private LocalDate diaryDate;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "日记内容")
    private String content;


    @Schema(description = "字数")
    private Integer wordCount;

    @Schema(description = "城市名")
    private String name;

    @Schema(description = "区级")
    private String adm2;

    @Schema(description = "天气")
    private String text;

    @Schema(description = "温度")
    private String temp;

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
