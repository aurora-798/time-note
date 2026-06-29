package com.note.ai.rerank;

import com.note.ai.config.RerankProperties;
import com.note.ai.rerank.dto.RerankRequest;
import com.note.ai.rerank.dto.RerankResponse;
import jakarta.annotation.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class DashScopeRerankClient {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RerankProperties rerankProperties;

    /**
     * 调用 DashScope OpenAI 兼容 rerank 接口，返回按相关度降序排列的结果。
     */
    public List<RerankResponse.RerankResultItem> rerank(String query, List<String> documents) {
        String url = normalizeBaseUrl(rerankProperties.getBaseUrl()) + "/reranks";

        RerankRequest request = new RerankRequest(
                rerankProperties.getModel(),
                query,
                documents,
                rerankProperties.getInstruct()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(rerankProperties.getApiKey());

        HttpEntity<RerankRequest> entity = new HttpEntity<>(request, headers);
        RerankResponse response = restTemplate.postForObject(url, entity, RerankResponse.class);
        if (response == null || response.getResults() == null) {
            return List.of();
        }
        return response.getResults();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
