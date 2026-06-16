package com.note.entity.request.diarybook;

import lombok.Data;

@Data
public class SysDiaryBookVerifyRequest {

    private Long bookId;

    private String password;
}
