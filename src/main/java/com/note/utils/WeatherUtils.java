package com.note.utils;


import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.note.entity.vo.weather.HeFengGeoResp;
import com.note.entity.vo.weather.HeFengWeatherNowResp;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
public class WeatherUtils {

    @Value("${weather.key}")
    private String hefengKey;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 经纬度 → 城市信息，key从请求头Authorization Bearer读取
     * @param lng 经度
     * @param lat 纬度
     * @return 省市区信息
     */
    public HeFengGeoResp.Location gpsToCity(double lng, double lat){
        // 1. 拼接第三方接口地址：不再拼接 &key=xxx，key放请求头传给和风
        String url = "https://nq52qdgqrv.re.qweatherapi.com/geo/v2/city/lookup?location=" + lng + "," + lat + "&key=" + hefengKey;
        ResponseEntity<byte[]> bytesData = restTemplate.getForEntity(url, byte[].class);
        byte[] bodyBytes = bytesData.getBody();
        if (bodyBytes == null) {
            throw new RuntimeException("和风接口返回空数据");
        }
        String jsonStr = decodeResponseBody(bodyBytes);
        // 2. 解析返回实体
        HeFengGeoResp resp = JSONUtil.toBean(jsonStr, HeFengGeoResp.class);
        // 和风code=200代表成功
        if (!"200".equals(resp.getCode()) || resp.getLocation() == null || resp.getLocation().isEmpty()) {
            throw new RuntimeException("经纬度解析城市失败，和风返回code：" + resp.getCode());
        }
        return resp.getLocation().getFirst();
    }


    /**
     * 根据城市ID/经纬度查询实时天气
     * @param location 城市ID 或 经度,纬度
     * @return 实时天气完整返回体
     */
    public HeFengWeatherNowResp getWeatherNow(String location){
        // 1. 拼接第三方接口地址
        String url = "https://nq52qdgqrv.re.qweatherapi.com/v7/weather/now?location=" + location + "&key=" + hefengKey;
        ResponseEntity<byte[]> bytesData = restTemplate.getForEntity(url, byte[].class);
        byte[] bodyBytes = bytesData.getBody();
        if (bodyBytes == null || bodyBytes.length == 0) {
            throw new RuntimeException("和风实时天气接口返回空数据");
        }
        String json = decodeResponseBody(bodyBytes);

        // 5. Hutool JSON解析为实体
        HeFengWeatherNowResp resp = JSONUtil.toBean(json, HeFengWeatherNowResp.class);

        // 6. 业务状态校验
        if (!"200".equals(resp.getCode()) || resp.getNow() == null) {
            throw new RuntimeException("查询实时天气失败，接口返回code=" + resp.getCode());
        }
        return resp;
    }

    /** 兼容 gzip 与明文 JSON：RestTemplate 有时会自动解压，重复解压会产生乱码 */
    private String decodeResponseBody(byte[] bodyBytes) {
        if (bodyBytes.length >= 2 && (bodyBytes[0] == (byte) 0x1f) && (bodyBytes[1] == (byte) 0x8b)) {
            return ZipUtil.unGzip(bodyBytes, CharsetUtil.UTF_8);
        }
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }
}
