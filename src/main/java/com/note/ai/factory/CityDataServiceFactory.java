package com.note.ai.factory;

import com.note.ai.service.CityDataService;
import com.note.ai.tools.CityDataTool;
import com.note.ai.tools.ToolManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CityDataServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ToolManager toolManager;


    @Bean
    public CityDataService cityDataService() {
        return AiServices.builder(CityDataService.class)
                .chatModel(chatModel)
                .tools(toolManager.getAllTools())
                .build();
    }
}
