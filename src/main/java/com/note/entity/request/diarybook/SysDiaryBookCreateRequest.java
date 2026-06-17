package com.note.entity.request.diarybook;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建日记本请求")
public class SysDiaryBookCreateRequest {
    private String name;

    private String cover;

    private String font;

    private Integer encrypted;

    private String password;

    @Schema(description = "封面文件名")
    private String coverFileName;

    @Schema(description = "封面文件大小（字节）")
    private Long coverFileSize;

    @Schema(description = "封面文件后缀")
    private String coverSuffix;
}
