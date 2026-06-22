package com.note.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

public interface DiaryRagSearchService {

    @SystemMessage(fromResource = "prompt/sys_diary_search_rag_prompt.txt")
    Flux<String> chatDiary(@V("context") String context,
                           @V("currentDate") String currentDate,
                           @V("yesterdayDate") String yesterdayDate,
                           @V("retrievalCount") int retrievalCount,
                           @UserMessage String userMessage);
}
