package com.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.constant.AiChatMessageConstant;
import com.note.constant.ResultCode;
import com.note.entity.AiChatMessage;
import com.note.entity.AiChatSession;
import com.note.entity.vo.chat.AiChatMessageVo;
import com.note.entity.vo.chat.AiChatSessionVo;
import com.note.exception.BusinessException;
import com.note.mapper.AiChatMessageMapper;
import com.note.mapper.AiChatSessionMapper;
import com.note.service.AiChatSessionService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatSessionServiceImpl implements AiChatSessionService {

    private static final String DEFAULT_TITLE = "新对话";

    @Resource
    private AiChatSessionMapper sessionMapper;

    @Resource
    private AiChatMessageMapper messageMapper;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Override
    public Long getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId == null) {
            AiChatSession session = new AiChatSession();
            session.setUserId(userId);
            session.setTitle(DEFAULT_TITLE);
            sessionMapper.insert(session);
            return session.getId();
        }
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return session.getId();
    }

    @Override
    public List<AiChatMessage> listRecentMessages(Long sessionId, int limit) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getCreateTime)
                .last("LIMIT " + Math.max(limit, 1));
        List<AiChatMessage> latest = messageMapper.selectList(wrapper);
        return latest.reversed();
    }

    @Override
    public List<AiChatMessage> loadRecentMessages(Long sessionId, MessageWindowChatMemory chatMemory,
                                                  int limit) {
        List<AiChatMessage> aiChatMessages = listRecentMessages(sessionId, limit);

        chatMemory.clear();
        for (AiChatMessage message : aiChatMessages) {
            String role = message.getRole();
            String content = message.getContent();
            if (AiChatMessageConstant.USER_MESSAGE_TYPE.equals(role)) {
                chatMemory.add(UserMessage.userMessage(content));
            } else {
                chatMemory.add(AiMessage.aiMessage(content));
            }
        }
        return aiChatMessages;
    }

    @Override
    public void appendUserMessage(Long sessionId, Long userId, String content, String searchQuery) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(AiChatMessageConstant.USER_MESSAGE_TYPE);
        message.setContent(content);
        message.setSearchQuery(searchQuery);
        messageMapper.insert(message);
        touchSession(sessionId);
    }

    @Override
    public void appendAssistantMessage(Long sessionId, Long userId, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(AiChatMessageConstant.ASSISTANT_MESSAGE_TYPE);
        message.setContent(truncate(content, ASSISTANT_STORE_MAX));
        messageMapper.insert(message);
        touchSession(sessionId);
    }

    @Override
    public void touchSession(Long sessionId) {
        AiChatSession session = new AiChatSession();
        session.setId(sessionId);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public List<AiChatMessageVo> listMessages(Long sessionId, Long userId) {
        assertSessionOwner(sessionId, userId);
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper).stream()
                .map(msg -> AiChatMessageVo.builder()
                        .id(msg.getId())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .createTime(msg.getCreateTime())
                        .build())
                .toList();
    }

    @Override
    public List<AiChatSessionVo> listSessions(Long userId, int limit) {
        int size = limit > 0 ? limit : SESSION_LIST_LIMIT;
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getUpdateTime)
                .last("LIMIT " + size);
        return sessionMapper.selectList(wrapper).stream()
                .map(session -> {
                    Long count = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
                            .eq(AiChatMessage::getSessionId, session.getId()));
                    return AiChatSessionVo.builder()
                            .sessionId(String.valueOf(session.getId()))
                            .title(session.getTitle())
                            .updateTime(session.getUpdateTime())
                            .messageCount(count)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        assertSessionOwner(sessionId, userId);
        sessionMapper.deleteById(sessionId);
        clearChatMemory(String.valueOf(sessionId));
    }

    @Override
    public void clearChatMemory(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        redisChatMemoryStore.deleteMessages(sessionId);
    }

    @Override
    public void updateTitleIfDefault(Long sessionId, String userMessage) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !DEFAULT_TITLE.equals(session.getTitle())) {
            return;
        }
        String title = StrUtil.sub(StrUtil.trim(userMessage), 0, 30);
        if (StrUtil.isBlank(title)) {
            return;
        }
        AiChatSession update = new AiChatSession();
        update.setId(sessionId);
        update.setTitle(title);
        sessionMapper.updateById(update);
    }

    private void assertSessionOwner(Long sessionId, Long userId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}
