package com.note.entity.request.diary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "编辑日记请求")
public class SysDiaryEditRequest {

    @Schema(description = "日记本 ID")
    private Long bookId;

    @Schema(description = "日记 ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "日记内容")
    private String content;
}
