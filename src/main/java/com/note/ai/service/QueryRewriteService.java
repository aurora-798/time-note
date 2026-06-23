package com.note.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface QueryRewriteService {

    @SystemMessage(fromResource = "prompt/query_rewrite_prompt.txt")
    String rewrite(@V("conversationHistory") String conversationHistory,
                   @V("currentDate") String currentDate,
                   @V("yesterdayDate") String yesterdayDate,
                   @UserMessage String userMessage);
}
