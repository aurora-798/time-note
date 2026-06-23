package com.note.ai.runner;

import com.note.ai.utils.RagUtils;
import com.note.ai.config.QdrantProperties;
import com.note.entity.SysDiary;
import com.note.mapper.SysDiaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "langchain4j.qdrant.reindex-on-startup", havingValue = "true")
public class VectorReindexRunner implements ApplicationRunner {

    private final SysDiaryMapper sysDiaryMapper;
    private final RagUtils ragUtils;
    private final QdrantProperties qdrantProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始全量重建 Qdrant hybrid 索引，collection={}", qdrantProperties.getCollectionName());
        List<SysDiary> diaries = sysDiaryMapper.selectList(null);
        int success = 0;
        int failed = 0;
        for (SysDiary diary : diaries) {
            try {
                ragUtils.embeddingUpdateTextAndStore(diary);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("重建索引失败，diaryId={}", diary.getId(), e);
            }
        }
        log.info("Qdrant hybrid 索引重建完成，成功 {} 条，失败 {} 条", success, failed);
    }
}
