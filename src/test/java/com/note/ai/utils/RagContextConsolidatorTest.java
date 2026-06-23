package com.note.ai.utils;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagContextConsolidatorTest {

    private static Metadata metadata(Map<String, String> values) {
        Metadata metadata = new Metadata();
        values.forEach(metadata::put);
        return metadata;
    }

    @Test
    void consolidate_mergesSameDiaryChunksByIndex() {
        Metadata meta = metadata(Map.of(
                "diaryId", "d1",
                "bookName", "我的日常",
                "title", "生病了",
                "diaryDate", "2026-06-22",
                "index", "0",
                "_retrievalScore", "0.03"
        ));
        Metadata metaShort = metadata(Map.of(
                "diaryId", "d1",
                "index", "1",
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
                RagContextConsolidator.consolidate(List.of(partial, full));

        assertEquals(1, diaries.size());
        assertEquals("我的日常", diaries.get(0).bookName());
        assertTrue(diaries.get(0).bodyText().contains("今天很难受"));
        assertTrue(diaries.get(0).bodyText().contains("拖延症"));
        assertEquals(0.03, diaries.get(0).retrievalScore());
    }

    @Test
    void consolidate_mergesChunksByIndexWithOverlapTrim() {
        Metadata meta0 = metadata(Map.of("diaryId", "d1", "index", "0"));
        Metadata meta1 = metadata(Map.of("diaryId", "d1", "index", "1", "_retrievalScore", "0.05"));

        TextSegment chunk0 = TextSegment.from(
                "日记正文：今天很难受。通过放弃考研也印证了自己有拖延症。", meta0);
        TextSegment chunk1 = TextSegment.from(
                "通过放弃考研也印证了自己有拖延症。后来又想明白了。", meta1);

        List<RagContextConsolidator.DiaryContext> diaries =
                RagContextConsolidator.consolidate(List.of(chunk1, chunk0));

        assertEquals(1, diaries.size());
        assertEquals(
                "今天很难受。通过放弃考研也印证了自己有拖延症。后来又想明白了。",
                diaries.get(0).bodyText()
        );
    }

    @Test
    void consolidate_fallsBackToLongestChunkWhenIndexMissing() {
        Metadata meta = metadata(Map.of(
                "diaryId", "d1",
                "bookName", "我的日常",
                "title", "生病了",
                "_retrievalScore", "0.03"
        ));
        Metadata metaPartial = metadata(Map.of("diaryId", "d1", "_retrievalScore", "0.02"));

        TextSegment full = TextSegment.from("""
                日记本名称：我的日常
                日记标题：生病了
                日记正文：完整正文。
                """, meta);
        TextSegment partial = TextSegment.from("完整正文。", metaPartial);

        List<RagContextConsolidator.DiaryContext> diaries =
                RagContextConsolidator.consolidate(List.of(partial, full));

        assertEquals(1, diaries.size());
        assertEquals("我的日常", diaries.get(0).bookName());
        assertEquals("完整正文。", diaries.get(0).bodyText());
    }

    @Test
    void consolidate_preservesRetrievalOrder() {
        Metadata longMeta = metadata(Map.of("diaryId", "long", "_retrievalScore", "0.03"));
        Metadata shortMeta = metadata(Map.of("diaryId", "short", "_retrievalScore", "0.08"));

        TextSegment shortDiary = TextSegment.from("短", shortMeta);
        TextSegment longDiary = TextSegment.from("长日记正文", longMeta);

        List<RagContextConsolidator.DiaryContext> diaries =
                RagContextConsolidator.consolidate(List.of(shortDiary, longDiary));

        assertEquals("short", diaries.get(0).diaryId());
        assertEquals("long", diaries.get(1).diaryId());
    }

    @Test
    void limit_truncatesAfterConsolidate() {
        var d1 = new RagContextConsolidator.DiaryContext(
                "d1", "", "", "", "", "", "", "", "", "a", 0.1);
        var d2 = new RagContextConsolidator.DiaryContext(
                "d2", "", "", "", "", "", "", "", "", "b", 0.2);

        List<RagContextConsolidator.DiaryContext> limited =
                RagContextConsolidator.limit(List.of(d1, d2), 1);

        assertEquals(1, limited.size());
        assertEquals("d1", limited.get(0).diaryId());
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
    void withRetrievalScore_supportsFutureRerank() {
        var original = new RagContextConsolidator.DiaryContext(
                "d1", "时光笔录", "开发进度", "2026-06-21",
                "", "", "", "", "153", "正文", 0.1
        );

        var reranked = original.withRetrievalScore(0.95);

        assertEquals(0.95, reranked.retrievalScore());
        assertEquals("d1", reranked.diaryId());
        assertEquals("正文", reranked.bodyText());
    }
}
