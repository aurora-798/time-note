package com.note.ai.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class TestAiServiceTest {

    @Resource
    private TestAiService testAiService;

    @Test
    void testGenerate() {
        String hello = testAiService.test("hello");
        System.out.println(hello);
    }

}