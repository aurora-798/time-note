package com.note.ai.service;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface TestAiService {
    @SystemMessage("You are a polite assistant")
    String test(String userMessage);
}
