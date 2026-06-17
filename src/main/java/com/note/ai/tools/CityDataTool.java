package com.note.ai.tools;

import cn.hutool.core.date.DateUtil;
import com.note.ai.model.CityData;
import com.note.entity.vo.weather.HeFengGeoResp;
import com.note.entity.vo.weather.HeFengWeatherNowResp;
import com.note.service.WeatherService;
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
    private WeatherService weatherService;

    @Tool("获取当前地理位置的天气信息")
    public CityData getWeatherNow(@P("经度") double lng, @P("纬度") double lat) {
        return weatherService.getWeatherNow(lng, lat);
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
