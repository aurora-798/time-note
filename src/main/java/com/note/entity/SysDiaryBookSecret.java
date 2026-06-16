package com.note.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("diary_book_secret")
@Builder
@Schema(description = "日记加密")
public class SysDiaryBookSecret implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 id")
    private Long id;

    @Schema(description = "日记本 id")
    private Long bookId;

    @Schema(description = "加密密码")
    private String secretHash;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
