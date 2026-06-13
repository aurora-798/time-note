package com.note.ai.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CityDataServiceTest {

    @Resource
    private CityDataService cityDataService;

    @Test
    void getNoNoteInfo() {
        cityDataService.getNoNoteInfo(119.12082223241518,36.771487991364275);
    }
}