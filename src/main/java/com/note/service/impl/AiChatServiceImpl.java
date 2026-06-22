package com.note.service.impl;

import cn.hutool.core.util.StrUtil;
import com.note.ai.model.RagSearchResult;
import com.note.ai.service.DiaryRagSearchService;
import com.note.ai.utils.RagUtils;
import com.note.ai.utils.TemporalQueryParser;
import com.note.constant.ResultCode;
import com.note.exception.BusinessException;
import com.note.service.AiChatService;
import com.note.utils.UserUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.format.DateTimeFormatter;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private DiaryRagSearchService diaryRagSearchService;

    @Resource
    private RagUtils ragUtils;

    @Resource
    private TemporalQueryParser temporalQueryParser;


    /**
     * 聊天
     * @param userId 用户 ID
     * @param userMessage 用户问题
     * @return 聊天结果
     */
    @Override
    public Flux<String> chat(String userId, String userMessage) {
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
        RagSearchResult searchResult = ragUtils.searchByUserMessage(userMessage, userId);
        if (searchResult.isSkipLlm()) {
            return Flux.just(searchResult.getDirectReply());
        }

        var today = temporalQueryParser.today();
        String currentDate = today.format(DATE_FORMAT);
        String yesterdayDate = today.minusDays(1).format(DATE_FORMAT);

        return diaryRagSearchService.chatDiary(
                searchResult.getContext(),
                currentDate,
                yesterdayDate,
                userMessage
        );
    }
}
