package com.note.ai.factory;

import com.note.ai.service.DiaryRagSearchService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiaryRagSearchServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Bean
    public DiaryRagSearchService generatorNoteContentService() {
        return AiServices.builder(DiaryRagSearchService.class)
                .chatModel(chatModel)
                .build();
    }
}
