package com.note.ai.service;

import com.note.ai.model.CityData;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CityDataService {

    @SystemMessage("""
    你是一个城市数据助手。根据用户提供的经纬度坐标，获取当前地理位置的天气信息。
    返回字段说明：
    - nowTime: 当前时间，格式 yyyy-MM-dd HH:mm:ss
    - week: 星期，如 1、2
    - city: 城市名称（如：潍坊）
    - weather: 天气状况（如：晴、多云、小雨）
    - temperature: 温度（如：25）
    - windForce: 风力等级（如：3）
    """)
    @UserMessage("请根据经度{{lng}}、纬度{{lat}}，查询该地点实时城市气象数据，按指定字段返回JSON")
    CityData getNoNoteInfo(@V("lng") double lng, @V("lat") double lat);

}
