package com.note.entity.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 和风实时天气返回实体
 */
@Data
public class HeFengWeatherNowResp {
    // 状态码 200成功
    private String code;
    // 更新时间
    private OffsetDateTime updateTime;
    // 天气详情链接
    private String fxLink;
    // 实时天气数据
    private Now now;
    // 来源许可信息
    private Refer refer;

    /**
     * 实时天气明细
     */
    @Data
    public static class Now {
        // 观测时间
        private String obsTime;
        // 温度
        private String temp;
        // 体感温度
        private String feelsLike;
        // 天气图标代码
        private String icon;
        // 天气文字描述
        private String text;
        // 风向角度
        private String wind360;
        // 风向名称
        private String windDir;
        // 风力等级
        private String windScale;
        // 风速 km/h
        private String windSpeed;
        // 湿度 %
        private String humidity;
        // 降水量 mm
        private String precip;
        // 气压 hPa
        private String pressure;
        // 能见度 km
        private String vis;
        // 云量 %
        private String cloud;
        // 露点温度
        private String dew;
    }

    /**
     * 数据来源、版权信息
     */
    @Data
    public static class Refer {
        private List<String> sources;
        private List<String> license;
    }
}