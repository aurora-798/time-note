package com.note.controller;

import cn.hutool.core.util.StrUtil;
import com.note.common.Result;
import com.note.entity.request.chat.AiChatRequest;
import com.note.entity.vo.chat.AiChatMessageVo;
import com.note.entity.vo.chat.AiChatSessionVo;
import com.note.entity.vo.chat.AiChatStreamResult;
import com.note.service.AiChatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AiChatController {

    public static final String SESSION_HEADER = "X-Chat-Session-Id";

    @Resource
    private AiChatService aiChatService;

    /**
     * 单轮聊天（SSE，向后兼容）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("userId") String userId,
                             @RequestParam("userMessage") String userMessage) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(userMessage)) {
            return Flux.just("参数错误");
        }
        return aiChatService.chat(userId, userMessage)
                .onErrorReturn("接口响应异常");
    }

    /**
     * 多轮 RAG 聊天（SSE）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatMultiTurn(@RequestBody AiChatRequest request,
                                      HttpServletResponse response) {
        if (request == null
                || StrUtil.isBlank(request.getUserId())
                || StrUtil.isBlank(request.getUserMessage())) {
            return Flux.just("参数错误");
        }
        Long sessionId = parseSessionId(request.getSessionId());
        AiChatStreamResult result = aiChatService.chatMultiTurn(
                request.getUserId(),
                request.getUserMessage(),
                sessionId);
        response.setHeader(SESSION_HEADER, String.valueOf(result.getSessionId()));
        return result.getStream().onErrorReturn("接口响应异常");
    }

    @GetMapping("/chat/sessions")
    public Result<List<AiChatSessionVo>> listSessions(@RequestParam("userId") String userId) {
        if (StrUtil.isBlank(userId)) {
            return Result.fail(400, "参数错误");
        }
        return Result.ok(aiChatService.listSessions(userId));
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    public Result<List<AiChatMessageVo>> listSessionMessages(@RequestParam("userId") String userId,
                                                           @PathVariable("sessionId") String sessionId) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(sessionId)) {
            return Result.fail(400, "参数错误");
        }
        return Result.ok(aiChatService.listSessionMessages(userId, Long.parseLong(sessionId)));
    }

    @DeleteMapping("/chat/sessions/{sessionId}")
    public Result<Void> deleteSession(@RequestParam("userId") String userId,
                                      @PathVariable("sessionId") String sessionId) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(sessionId)) {
            return Result.fail(400, "参数错误");
        }
        aiChatService.deleteSession(userId, Long.parseLong(sessionId));
        return Result.ok();
    }

    private Long parseSessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        try {
            return Long.parseLong(sessionId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
