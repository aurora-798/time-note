package com.note.ai.rerank;

import com.note.ai.config.RerankProperties;
import com.note.ai.rerank.dto.RerankResponse;
import com.note.ai.utils.RagContextConsolidator.DiaryContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DiaryRerankService {

    @Resource
    private RerankProperties rerankProperties;

    @Resource
    private DashScopeRerankClient dashScopeRerankClient;

    /**
     * 对候选日记重排序，低于 min-score 的条目直接过滤。
     * API 异常时回退为 rerank 前的列表。
     */
    public List<DiaryContext> rerank(String query, List<DiaryContext> candidates) {
        if (!rerankProperties.isEnabled() || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }

        List<String> passages = candidates.stream()
                .map(diary -> DiaryPassageBuilder.build(diary, rerankProperties.getMaxPassageChars()))
                .toList();

        try {
            List<RerankResponse.RerankResultItem> results =
                    dashScopeRerankClient.rerank(query, passages);

            List<DiaryContext> reranked = new ArrayList<>();
            double minScore = rerankProperties.getMinScore();
            for (RerankResponse.RerankResultItem item : results) {
                if (item.getRelevanceScore() < minScore) {
                    continue;
                }
                int index = item.getIndex();
                if (index < 0 || index >= candidates.size()) {
                    continue;
                }
                reranked.add(candidates.get(index).withRetrievalScore(item.getRelevanceScore()));
            }

            log.info("rerank candidates={} kept={} minScore={}",
                    candidates.size(), reranked.size(), minScore);
            return reranked;
        } catch (Exception e) {
            log.warn("rerank failed, fallback to pre-rerank order: {}", e.getMessage());
            return candidates;
        }
    }
}
