package com.note.entity.vo.diary;

import lombok.Data;

@Data
public class SysDiaryWeatherVo {
    // 当前时间
    private String nowTime;
    // 星期
    private Integer week;
    // 所在城市
    private String city;

    // 区级
    private String name;

    // 天气情况
    private String weather;
    // 温度
    private String temperature;
    // 风力
    private String windForce;
}
