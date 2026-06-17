package com.note.entity.vo.diarybook;

import com.note.entity.SysDiaryBook;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class SysDiaryBookListVo {
    // 日记本信息
    private SysDiaryBookFindVo sysDiaryBookFindVo;
    // 日记本数量
    private long entryCount;
}
