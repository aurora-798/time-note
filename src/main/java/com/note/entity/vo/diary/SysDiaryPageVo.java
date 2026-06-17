package com.note.entity.vo.diary;

import com.note.entity.SysDiary;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SysDiaryPageVo {

    private List<SysDiary> records;

    private Long total;

    private Long pages;

    private Long current;

    private Long size;
}
