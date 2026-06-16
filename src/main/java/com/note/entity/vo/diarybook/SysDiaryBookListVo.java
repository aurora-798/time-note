package com.note.entity.vo.diarybook;

import com.note.entity.SysDiaryBook;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SysDiaryBookListVo {
    // 日记本列表
    private List<SysDiaryBook> list;
    // 日记本数量
    private Integer entryCount;
}
