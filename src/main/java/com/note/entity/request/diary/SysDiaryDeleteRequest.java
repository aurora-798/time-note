package com.note.entity.request.diary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "删除日记请求")
public class SysDiaryDeleteRequest {

    @Schema(description = "日记ID")
    private Long id;

    @Schema(description = "加密日记本密码（删除加密本内日记时必填）")
    private String password;
}
