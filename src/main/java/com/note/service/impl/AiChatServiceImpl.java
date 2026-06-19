package com.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.note.ai.service.DiaryRagSearchService;
import com.note.ai.utils.RagUtils;
import com.note.constant.ResultCode;
import com.note.exception.BusinessException;
import com.note.service.AiChatService;
import com.note.utils.UserUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private DiaryRagSearchService diaryRagSearchService;

    @Resource
    private RagUtils ragUtils;


    /**
     * 聊天
     * @param userId 用户 ID
     * @param userMessage 用户问题
     * @return 聊天结果
     */
    @Override
    public String chat(String userId, String userMessage) {
        Long currentUserId = UserUtils.currentUserId();
        if(currentUserId == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if(userId == null || StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if(!currentUserId.equals(Long.parseLong(userId))) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        // 从向量数据库中检索答案
        String ragArticleList = ragUtils.searchAnswerByUserMessage(userMessage, userId);
        // 调用 LLM 返回结果
        return diaryRagSearchService.chatDiary(ragArticleList, userMessage);
    }
}
