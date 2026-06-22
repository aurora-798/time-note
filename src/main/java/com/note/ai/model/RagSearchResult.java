package com.note.ai.model;

import lombok.Getter;

@Getter
public class RagSearchResult {

    private final String context;
    private final String directReply;
    private final boolean skipLlm;

    private RagSearchResult(String context, String directReply, boolean skipLlm) {
        this.context = context;
        this.directReply = directReply;
        this.skipLlm = skipLlm;
    }

    public static RagSearchResult withContext(String context) {
        return new RagSearchResult(context, null, false);
    }

    public static RagSearchResult directReply(String message) {
        return new RagSearchResult(null, message, true);
    }
}
