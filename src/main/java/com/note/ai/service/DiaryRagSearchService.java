package com.note.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DiaryRagSearchService {

    @SystemMessage(fromResource = "prompt/sys_diary_search_rag_prompt.txt")
    String chatDiary(@V("context") String context, @UserMessage String userMessage);
}
