package com.note.ai.config;

import com.note.constant.RagSettingConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.rerank")
public class RerankProperties {

    private boolean enabled = true;

    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-api/v1";

    private String apiKey;

    private String model = "qwen3-rerank";

    private double minScore = RagSettingConstant.RERANK_MIN_SCORE;

    private int maxPassageChars = RagSettingConstant.RERANK_MAX_PASSAGE_CHARS;

    private String instruct =
            "根据用户的日记检索问题，找出真正包含相关经历或内容的日记。若用户询问的经历在日记中不存在，应给予低分。";
}
