package com.note.controller;

import cn.hutool.core.util.StrUtil;
import com.note.common.Result;
import com.note.entity.request.chat.AiChatDto;
import com.note.entity.vo.chat.AiChatVo;
import com.note.service.AiChatService;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;


    /**
     * 聊天
     * @return 返回 SSE 响应结果
     */
    @GetMapping(value = "/chat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("userId") String userId,
                             @RequestParam("userMessage") String userMessage) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(userMessage)) {
            return Flux.just("参数错误");
        }
        return aiChatService.chat(userId, userMessage)
                .onErrorReturn("接口响应异常");
    }
}
