package com.note.utils;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QiniuUtilsTest {

    @Resource
    private QiniuUtils qiniuUtils;

    @Test
    void uploadByBytes() {
        File file = new File("/Users/shuhang/Pictures/风景/壮阔-山峦-山脉.png");
        byte[] bytes = FileUtil.readBytes(file);
        String uuid = UUID.randomUUID().toString();
        String result = qiniuUtils.uploadByBytes(bytes,"test",uuid + ".png");
        System.out.println(result);
    }

    @Test
    void delete() {
        String result = qiniuUtils.delete("壮阔-山峦-山脉.png");
        System.out.println(result);
    }
}