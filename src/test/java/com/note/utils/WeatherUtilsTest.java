package com.note.utils;

import com.note.entity.vo.weather.HeFengGeoResp;
import com.note.entity.vo.weather.HeFengWeatherNowResp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WeatherUtilsTest {


    @Resource
    private WeatherUtils weatherUtil;

    @Test
    void gpsToCity() {
        HeFengGeoResp.Location location = weatherUtil.gpsToCity(119.12082223241518, 36.771487991364275);
        System.out.println(location);
    }

    @Test
    void getWeatherNow() {
        HeFengWeatherNowResp weatherNow = weatherUtil.getWeatherNow("101110101");
        System.out.println(weatherNow);
    }
}