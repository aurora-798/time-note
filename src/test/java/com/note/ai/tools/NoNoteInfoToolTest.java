package com.note.ai.tools;

import com.note.ai.model.CityData;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NoNoteInfoToolTest {

    @Resource
    private CityDataTool noNoteInfoTool;

    @Test
    void getWeatherNow() {
        CityData weatherNow = noNoteInfoTool.getWeatherNow(119.12082223241518, 36.771487991364275);
        System.out.println(weatherNow);
    }
}