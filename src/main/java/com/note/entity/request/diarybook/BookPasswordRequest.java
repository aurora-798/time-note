package com.note.entity.request.diarybook;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "加密日记本访问密码（非加密本可省略）")
public class BookPasswordRequest {

    @Schema(description = "日记本密码")
    private String password;
}
