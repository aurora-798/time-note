package com.note.ai.model;

import lombok.Builder;
import lombok.Data;

/**
 * 城市数据节点
 */
@Data
@Builder
public class CityData {

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
