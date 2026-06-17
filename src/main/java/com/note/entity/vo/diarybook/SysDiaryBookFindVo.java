package com.note.entity.vo.diarybook;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysDiaryBookFindVo {
    private Long id;

    private String name;

    private String cover;

    private String font;

    private Integer encrypted;
}
