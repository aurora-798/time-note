package com.note.ai.model;

import lombok.Builder;

/**
 * 城市数据节点
 */
@Builder
public class CityData {

    // 当前时间
    private String nowTime;
    // 星期
    private Integer week;
    // 所在城市
    private String city;
    // 天气情况
    private String weather;
    // 温度
    private String temperature;
    // 风力
    private String windForce;
}
