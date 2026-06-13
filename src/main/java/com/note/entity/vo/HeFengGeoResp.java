package com.note.entity.vo;

import lombok.Data;
import java.util.List;

// 和风天气查询城市信息
@Data
public class HeFengGeoResp {
    private String code;
    private List<Location> location;

    @Data
    public static class Location{
        private String id;          // Location_Id
        private String name;       // 城市名
        private String adm2;        // 区级
        private String adm1;        // 省级
        private String country;     // 国家
        private String lon;
        private String lat;
    }
}