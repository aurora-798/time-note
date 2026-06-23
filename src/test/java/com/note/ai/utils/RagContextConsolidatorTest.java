package com.note.ai.utils;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagContextConsolidatorTest {

    private static Metadata metadata(Map<String, String> values) {
        Metadata metadata = new Metadata();
        values.forEach(metadata::put);
        return metadata;
    }

    @Test
    void consolidate_mergesSameDiaryChunksAndKeepsLongestBody() {
        Metadata meta = metadata(Map.of(
                "diaryId", "d1",
                "bookName", "我的日常",
                "title", "生病了",
                "diaryDate", "2026-06-22",
                "_retrievalScore", "0.03"
        ));
        Metadata metaShort = metadata(Map.of(
                "diaryId", "d1",
                "_retrievalScore", "0.02"
        ));

        TextSegment full = TextSegment.from("""
                日记本名称：我的日常
                日记标题：生病了
                日记日期：2026-06-22
                日记正文：今天很难受。通过放弃考研也印证了自己有拖延症。
                日记字数：30
                """, meta);
        TextSegment partial = TextSegment.from("通过放弃考研也印证了自己有拖延症。", metaShort);

        List<RagContextConsolidator.DiaryContext> diaries =
                RagContextConsolidator.consolidate(List.of(partial, full), 5);

        assertEquals(1, diaries.size());
        assertEquals("我的日常", diaries.get(0).bookName());
        assertTrue(diaries.get(0).bodyText().contains("今天很难受"));
        assertTrue(diaries.get(0).bodyText().contains("拖延症"));
    }

    @Test
    void formatContext_usesStructuredHeaders() {
        String context = RagContextConsolidator.formatContext(List.of(
                new RagContextConsolidator.DiaryContext(
                        "d1", "时光笔录", "开发进度", "2026-06-21",
                        "潍坊", "奎文", "阴", "23", "153",
                        "接入 Qdrant。", 0.1
                )
        ));

        assertTrue(context.contains("日记本名称：时光笔录"));
        assertTrue(context.contains("日记标题：开发进度"));
        assertTrue(context.contains("日记日期：2026-06-21"));
        assertTrue(context.contains("地点：潍坊奎文"));
        assertTrue(context.contains("天气：阴，温度：23"));
        assertTrue(context.contains("日记字数：153"));
        assertTrue(context.contains("日记正文：接入 Qdrant。"));
    }

    @Test
    void consolidate_penalizesShortDiaryRanking() {
        Metadata longMeta = metadata(Map.of(
                "diaryId", "long",
                "wordCount", "153",
                "_retrievalScore", "0.03"
        ));
        Metadata shortMeta = metadata(Map.of(
                "diaryId", "short",
                "wordCount", "15",
                "_retrievalScore", "0.03"
        ));

        TextSegment longDiary = TextSegment.from("长日记正文", longMeta);
        TextSegment shortDiary = TextSegment.from("短", shortMeta);

        List<RagContextConsolidator.DiaryContext> diaries =
                RagContextConsolidator.consolidate(List.of(shortDiary, longDiary), 5);

        assertEquals("long", diaries.get(0).diaryId());
    }

    @Test
    void formatContext_marksPlanEntry() {
        String context = RagContextConsolidator.formatContext(List.of(
                new RagContextConsolidator.DiaryContext(
                        "d2", "时光笔录", "预完成开发计划", "2026-06-23",
                        "潍坊", "奎文", "多云", "20", "15",
                        "###完成query改写", 0.1
                )
        ));

        assertTrue(context.contains("记录类型：开发计划"));
    }

    @Test
    void isPlanLikeEntry_detectsPlanTitle() {
        var plan = new RagContextConsolidator.DiaryContext(
                "d2", "时光笔录", "预完成开发计划", "2026-06-23",
                "", "", "", "", "15", "###完成query改写", 0.1
        );
        var progress = new RagContextConsolidator.DiaryContext(
                "d1", "时光笔录", "开发进度", "2026-06-21",
                "", "", "", "", "153", "接入Qdrant，完成存储与基本的检索功能。", 0.2
        );

        assertTrue(RagContextConsolidator.isPlanLikeEntry(plan));
        assertFalse(RagContextConsolidator.isPlanLikeEntry(progress));
        assertTrue(RagContextConsolidator.isProgressLikeEntry(progress));
    }
}
