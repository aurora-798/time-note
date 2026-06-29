package com.note.ai.rerank;

import com.note.ai.config.RerankProperties;
import com.note.ai.rerank.dto.RerankResponse;
import com.note.ai.utils.RagContextConsolidator.DiaryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryRerankServiceTest {

    @Mock
    private RerankProperties rerankProperties;

    @Mock
    private DashScopeRerankClient dashScopeRerankClient;

    @InjectMocks
    private DiaryRerankService diaryRerankService;

    private DiaryContext diaryA;
    private DiaryContext diaryB;

    @BeforeEach
    void setUp() {
        diaryA = new DiaryContext(
                "d1", "时光笔录", "开发进度", "2026-06-21",
                "潍坊", "奎文", "阴", "23", "153", "接入 Qdrant。", 0.1
        );
        diaryB = new DiaryContext(
                "d2", "旅行", "海边", "2026-06-22",
                "", "", "", "", "80", "今天去了海边。", 0.05
        );
    }

    @Test
    void rerank_skipsWhenDisabled() {
        when(rerankProperties.isEnabled()).thenReturn(false);

        List<DiaryContext> result = diaryRerankService.rerank("query", List.of(diaryA, diaryB));

        assertEquals(2, result.size());
        verify(dashScopeRerankClient, never()).rerank(eq("query"), anyList());
    }

    @Test
    void rerank_filtersBelowMinScoreAndSortsByApiOrder() {
        when(rerankProperties.isEnabled()).thenReturn(true);
        when(rerankProperties.getMaxPassageChars()).thenReturn(1500);
        when(rerankProperties.getMinScore()).thenReturn(0.40);

        RerankResponse.RerankResultItem high = resultItem(1, 0.85);
        RerankResponse.RerankResultItem low = resultItem(0, 0.20);
        when(dashScopeRerankClient.rerank(eq("query"), anyList()))
                .thenReturn(List.of(high, low));

        List<DiaryContext> result = diaryRerankService.rerank("query", List.of(diaryA, diaryB));

        assertEquals(1, result.size());
        assertEquals("d2", result.get(0).diaryId());
        assertEquals(0.85, result.get(0).retrievalScore());
    }

    @Test
    void rerank_returnsEmptyWhenAllBelowThreshold() {
        when(rerankProperties.isEnabled()).thenReturn(true);
        when(rerankProperties.getMaxPassageChars()).thenReturn(1500);
        when(rerankProperties.getMinScore()).thenReturn(0.40);

        when(dashScopeRerankClient.rerank(eq("query"), anyList()))
                .thenReturn(List.of(resultItem(0, 0.10), resultItem(1, 0.15)));

        List<DiaryContext> result = diaryRerankService.rerank("query", List.of(diaryA, diaryB));

        assertTrue(result.isEmpty());
    }

    @Test
    void rerank_fallbackOnApiFailure() {
        when(rerankProperties.isEnabled()).thenReturn(true);
        when(rerankProperties.getMaxPassageChars()).thenReturn(1500);
        when(dashScopeRerankClient.rerank(eq("query"), anyList()))
                .thenThrow(new RuntimeException("api down"));

        List<DiaryContext> candidates = List.of(diaryA, diaryB);
        List<DiaryContext> result = diaryRerankService.rerank("query", candidates);

        assertEquals(candidates, result);
    }

    @Test
    void build_passageTruncatesToMaxChars() {
        String longBody = "x".repeat(2000);
        DiaryContext diary = new DiaryContext(
                "d3", "本", "标题", "2026-01-01",
                "", "", "", "", "2000", longBody, 0.1
        );

        String passage = DiaryPassageBuilder.build(diary, 100);

        assertEquals(100, passage.length());
        assertTrue(passage.startsWith("日记本名称：本"));
    }

    private static RerankResponse.RerankResultItem resultItem(int index, double score) {
        RerankResponse.RerankResultItem item = new RerankResponse.RerankResultItem();
        item.setIndex(index);
        item.setRelevanceScore(score);
        return item;
    }
}
