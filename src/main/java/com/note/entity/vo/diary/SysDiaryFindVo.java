package com.note.entity.vo.diary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SysDiaryFindVo {

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
}
