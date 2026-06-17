package com.note.service.impl;

import cn.hutool.core.date.DateUtil;
import com.note.ai.model.CityData;
import com.note.entity.vo.weather.HeFengGeoResp;
import com.note.entity.vo.weather.HeFengWeatherNowResp;
import com.note.service.WeatherService;
import com.note.utils.WeatherUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    private WeatherUtils weatherUtils;


    /**
     * 获取天气信息
     * @param lng 经度
     * @param lat 纬度
     * @return 天气信息
     */
    @Override
    public CityData getWeatherNow(double lng, double lat) {
        HeFengGeoResp.Location location = weatherUtils.gpsToCity(lng, lat);
        if(location == null || location.getId().isBlank()) return CityData.builder()
                .nowTime(null)
                .city("未知")
                .weather("未知")
                .temperature("未知")
                .windForce("未知").build();

        HeFengWeatherNowResp weatherNow = weatherUtils.getWeatherNow(location.getId());
        LocalDateTime localDateTime = weatherNow.getUpdateTime().toLocalDateTime();

        String localDateTimeFormat = DateUtil.format(localDateTime, "yyyy-MM-dd HH:mm:ss");
        return CityData.builder()
                .nowTime(localDateTimeFormat)
                .week(localDateTime.getDayOfWeek().getValue())
                .city(location.getAdm2())
                .weather(weatherNow.getNow().getText())
                .temperature(weatherNow.getNow().getTemp())
                .windForce(weatherNow.getNow().getWindScale()).build();
    }
}
