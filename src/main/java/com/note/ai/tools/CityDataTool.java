package com.note.ai.tools;

import cn.hutool.core.date.DateUtil;
import com.note.ai.model.CityData;
import com.note.entity.vo.HeFengGeoResp;
import com.note.entity.vo.HeFengWeatherNowResp;
import com.note.utils.WeatherUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CityDataTool implements BaseTool{

    @Resource
    private WeatherUtils weatherUtils;

    @Tool("获取当前地理位置的天气信息")
    public CityData getWeatherNow(@P("经度") double lng, @P("纬度") double lat) {
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

    @Override
    public String getToolName() {
        return "CityDataTool";
    }

    @Override
    public String getToolDisplayName() {
        return "城市信息查询工具";
    }
}
