package com.note.entity.request.diary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询日记请求")
public class SysDiaryPageRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;

    @Schema(description = "日记本ID（可选，不传则查全部）")
    private Long bookId;

    @Schema(description = "加密日记本密码（查询加密本时必填）")
    private String password;
}
