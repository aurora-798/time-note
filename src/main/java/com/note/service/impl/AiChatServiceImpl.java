package com.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.note.ai.factory.DiaryRagSearchServiceFactory;
import com.note.ai.model.RagSearchResult;
import com.note.ai.service.DiaryRagSearchService;
import com.note.ai.service.QueryRewriteService;
import com.note.ai.utils.ConversationFormatter;
import com.note.ai.utils.RagUtils;
import com.note.constant.ResultCode;
import com.note.entity.AiChatMessage;
import com.note.entity.vo.chat.AiChatStreamResult;
import com.note.exception.BusinessException;
import com.note.service.AiChatService;
import com.note.service.AiChatSessionService;
import com.note.utils.UserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private DiaryRagSearchServiceFactory diaryRagSearchServiceFactory;

    @Resource
    private QueryRewriteService queryRewriteService;

    @Resource
    private RagUtils ragUtils;

    @Resource
    private AiChatSessionService chatSessionService;

    @Override
    public Flux<String> chat(String userId, String userMessage) {
        requireCurrentUser(userId);
        String searchQuery = resolveSearchQuery(Collections.emptyList(), userMessage);
        log.info("single-turn chat original=[{}] rewritten=[{}]", userMessage, searchQuery);

        RagSearchResult searchResult = ragUtils.searchByUserMessage(searchQuery, userId);
        if (searchResult.isSkipLlm()) {
            return Flux.just(searchResult.getDirectReply());
        }

        var today = LocalDate.now(SERVER_ZONE);
        String currentDate = today.format(DATE_FORMAT);
        String yesterdayDate = today.minusDays(1).format(DATE_FORMAT);

        DiaryRagSearchService diaryRagSearchService = diaryRagSearchServiceFactory.createStateless();
        return diaryRagSearchService.chatDiary(
                searchResult.getContext(),
                currentDate,
                yesterdayDate,
                searchResult.getRetrievalCount(),
                userMessage
        );
    }

    @Override
    public AiChatStreamResult chatMultiTurn(String userId, String userMessage, Long sessionId) {
        Long currentUserId = requireCurrentUser(userId);
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        Long sid = chatSessionService.getOrCreateSession(currentUserId, sessionId);
        String sessionKey = String.valueOf(sid);

        List<AiChatMessage> history = chatSessionService.listRecentMessages(
                sid, AiChatSessionService.REWRITE_HISTORY_LIMIT);
        String searchQuery = resolveSearchQuery(history, userMessage);

        log.info("multi-turn chat sessionId={} historyTurns={} original=[{}] rewritten=[{}]",
                sid, history.size(), userMessage, searchQuery);

        chatSessionService.updateTitleIfDefault(sid, userMessage);

        RagSearchResult searchResult = ragUtils.searchByUserMessage(searchQuery, userId);

        var today = LocalDate.now(SERVER_ZONE);
        String currentDate = today.format(DATE_FORMAT);
        String yesterdayDate = today.minusDays(1).format(DATE_FORMAT);

        if (searchResult.isSkipLlm()) {
            String directReply = searchResult.getDirectReply();
            chatSessionService.appendUserMessage(sid, currentUserId, userMessage, searchQuery);
            chatSessionService.appendAssistantMessage(sid, currentUserId, directReply);
            chatSessionService.clearChatMemory(sessionKey);
            return new AiChatStreamResult(sid, Flux.just(directReply));
        }

        DiaryRagSearchService diaryRagSearchService =
                diaryRagSearchServiceFactory.createWithMemory(sessionKey);

        StringBuilder buffer = new StringBuilder();
        Flux<String> stream = diaryRagSearchService.chatDiary(
                        searchResult.getContext(),
                        currentDate,
                        yesterdayDate,
                        searchResult.getRetrievalCount(),
                        userMessage)
                .doOnNext(buffer::append)
                .doOnComplete(() -> {
                    chatSessionService.appendUserMessage(sid, currentUserId, userMessage, searchQuery);
                    chatSessionService.appendAssistantMessage(sid, currentUserId, buffer.toString());
                });

        return new AiChatStreamResult(sid, stream);
    }

    @Override
    public List<com.note.entity.vo.chat.AiChatSessionVo> listSessions(String userId) {
        Long currentUserId = requireCurrentUser(userId);
        return chatSessionService.listSessions(currentUserId, AiChatSessionService.SESSION_LIST_LIMIT);
    }

    @Override
    public List<com.note.entity.vo.chat.AiChatMessageVo> listSessionMessages(String userId, Long sessionId) {
        Long currentUserId = requireCurrentUser(userId);
        return chatSessionService.listMessages(sessionId, currentUserId);
    }

    @Override
    public void deleteSession(String userId, Long sessionId) {
        Long currentUserId = requireCurrentUser(userId);
        chatSessionService.deleteSession(sessionId, currentUserId);
    }

    private String resolveSearchQuery(List<AiChatMessage> history, String userMessage) {
        try {
            var today = LocalDate.now(SERVER_ZONE);
            String currentDate = today.format(DATE_FORMAT);
            String yesterdayDate = today.minusDays(1).format(DATE_FORMAT);
            String rewritten = queryRewriteService.rewrite(
                    ConversationFormatter.formatForRewrite(history),
                    currentDate,
                    yesterdayDate,
                    userMessage);
            if (StrUtil.isBlank(rewritten)) {
                return userMessage;
            }
            return StrUtil.trim(rewritten);
        } catch (Exception e) {
            log.warn("query rewrite failed, fallback to original message: {}", e.getMessage());
            return userMessage;
        }
    }

    private Long requireCurrentUser(String userId) {
        Long currentUserId = UserUtils.currentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (!currentUserId.equals(Long.parseLong(userId))) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return currentUserId;
    }
}
