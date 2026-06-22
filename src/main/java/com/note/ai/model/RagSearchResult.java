package com.note.ai.model;

import lombok.Getter;

@Getter
public class RagSearchResult {

    private static final String NO_MATCH_REPLY =
            "检索全部日记文档后未查询到和该问题相关的记录，请调整提问关键词或询问其他日记内容";

    private final String context;
    private final String directReply;
    private final boolean skipLlm;
    private final int retrievalCount;

    private RagSearchResult(String context, String directReply, boolean skipLlm, int retrievalCount) {
        this.context = context;
        this.directReply = directReply;
        this.skipLlm = skipLlm;
        this.retrievalCount = retrievalCount;
    }

    public static RagSearchResult withContext(String context, int retrievalCount) {
        return new RagSearchResult(context, null, false, retrievalCount);
    }

    public static RagSearchResult directReply(String message) {
        return new RagSearchResult(null, message, true, 0);
    }

    public static RagSearchResult noMatchReply() {
        return directReply(NO_MATCH_REPLY);
    }
}
