package com.note.service;

import com.note.ai.model.CityData;

public interface WeatherService {

    /**
     * 获取天气信息
     * @param lng 经度
     * @param lat 纬度
     * @return 天气信息
     */
    CityData getWeatherNow(double lng, double lat);
}
