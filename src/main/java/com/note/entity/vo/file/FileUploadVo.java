package com.note.entity.vo.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVo {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String suffix;
}
