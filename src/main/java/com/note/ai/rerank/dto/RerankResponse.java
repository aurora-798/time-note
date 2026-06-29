package com.note.ai.rerank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RerankResponse {

    private List<RerankResultItem> results;

    @Data
    public static class RerankResultItem {

        private int index;

        @JsonProperty("relevance_score")
        private double relevanceScore;
    }
}
