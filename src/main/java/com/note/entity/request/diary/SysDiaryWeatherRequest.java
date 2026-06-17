package com.note.entity.request.diary;

import lombok.Data;

@Data
public class SysDiaryWeatherRequest {

    // 经度
    private Double lng;

    // 纬度
    private Double lat;
}
