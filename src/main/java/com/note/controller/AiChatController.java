package com.note.controller;

import com.note.common.Result;
import com.note.entity.request.chat.AiChatDto;
import com.note.entity.vo.chat.AiChatVo;
import com.note.service.AiChatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    /**
     * 聊天
     * @param aiChatDto
     * @return
     */
    @PostMapping
    public Result<AiChatVo> chat(@RequestBody AiChatDto aiChatDto) {
        String userId = aiChatDto.getUserId();
        String userMessage = aiChatDto.getUserMessage();
        String chat = aiChatService.chat(userId, userMessage);
        return Result.ok(new AiChatVo(chat));
    }
}
