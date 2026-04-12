package com.inote.service.retrieval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inote.config.RagProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RerankService {

    private final RagProperties ragProperties;
    private final String apiKey;
    private final RestClient restClient;

    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-reranking/generation";

    public RerankService(
            RagProperties ragProperties,
            @Value("${spring.ai.dashscope.api-key:${spring.ai.openai.api-key:}}") String apiKey) {
        this.ragProperties = ragProperties;
        this.apiKey = apiKey;

        // Configure timeout for HTTP requests
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(RERANK_URL)
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        if (candidates.size() <= 1) {
            return candidates;
        }

        try {
            log.debug("Reranking {} candidates with query: {}", candidates.size(), query);

            List<String> texts = candidates.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

            RerankRequest request = new RerankRequest();
            request.setModel(ragProperties.getRerankModel());
            RerankRequest.Input input = new RerankRequest.Input();
            input.setQuery(query);
            input.setDocuments(texts);
            request.setInput(input);
            RerankRequest.Parameters parameters = new RerankRequest.Parameters();
            parameters.setTopN(Math.min(topN, candidates.size()));
            request.setParameters(parameters);

            RerankResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.getOutput() == null
                    || response.getOutput().getResults() == null) {
                log.warn("Rerank API returned empty response, using original order");
                return candidates.stream().limit(topN).collect(Collectors.toList());
            }

            // 按 relevance_score 降序排列，取 topN
            List<RerankResponse.Result> results = response.getOutput().getResults();
            results.sort(Comparator.comparingDouble(RerankResponse.Result::getRelevanceScore).reversed());

            List<Document> reranked = new ArrayList<>();
            for (RerankResponse.Result result : results) {
                int index = result.getIndex();
                if (index >= 0 && index < candidates.size()) {
                    reranked.add(candidates.get(index));
                }
            }

            log.info("Reranking complete, returned {} documents", reranked.size());
            return reranked;

        } catch (Exception e) {
            log.warn("Rerank failed, using original order: {}", e.getMessage());
            return candidates.stream().limit(topN).collect(Collectors.toList());
        }
    }

    // DashScope Rerank API 请求体
    @Data
    static class RerankRequest {
        private String model;
        private Input input;
        private Parameters parameters;

        @Data
        static class Input {
            private String query;
            private List<String> documents;
        }

        @Data
        static class Parameters {
            @JsonProperty("top_n")
            private int topN;
        }
    }

    // DashScope Rerank API 响应体
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RerankResponse {
        private Output output;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Output {
            private List<Result> results;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Result {
            private int index;
            @JsonProperty("relevance_score")
            private double relevanceScore;
        }
    }
}
