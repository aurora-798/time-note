package com.note.ai.context;

import com.note.ai.model.CityData;
import com.note.entity.SysUser;
import dev.langchain4j.model.output.TokenUsage;

public class DiaryContext {
    // 前置信息
    private CityData noNoteInfo;
    // 用户信息
    private SysUser user;
    // 用户原始输入
    private String rawInput;
    // 节点输出中间数据
//    private List<DiaryEvent> eventList;
//    private EmotionInfo emotionInfo;
    private String stylePrompt;
    // 最终结果
    private String finalDiaryContent;
    // token统计
    private TokenUsage totalToken;
}