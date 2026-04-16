// 声明当前源文件的包。
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class RerankService {

    // 声明当前字段。
    private final RagProperties ragProperties;
    // 声明当前字段。
    private final String apiKey;
    // 声明当前字段。
    private final RestClient restClient;

    // 处理当前代码结构。
    private static final String RERANK_URL =
            // 声明当前字段。
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-reranking/generation";

    /**
     * 描述 `RerankService` 操作。
     *
     * @param ragProperties 输入参数 `ragProperties`。
     * @param apiKey 输入参数 `apiKey`。
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    public RerankService(
            // 处理当前代码结构。
            RagProperties ragProperties,
            // 应用当前注解。
            @Value("${spring.ai.dashscope.api-key:${spring.ai.openai.api-key:}}") String apiKey) {
        // 执行当前语句。
        this.ragProperties = ragProperties;
        // 执行当前语句。
        this.apiKey = apiKey;

        // 执行当前语句。
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 执行当前语句。
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        // 执行当前语句。
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        // 处理当前代码结构。
        this.restClient = RestClient.builder()
                // 处理当前代码结构。
                .baseUrl(RERANK_URL)
                // 处理当前代码结构。
                .requestFactory(requestFactory)
                // 处理当前代码结构。
                .defaultHeader("Authorization", "Bearer " + apiKey)
                // 处理当前代码结构。
                .defaultHeader("Content-Type", "application/json")
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `rerank` 操作。
     *
     * @param query 输入参数 `query`。
     * @param candidates 输入参数 `candidates`。
     * @param topN 输入参数 `topN`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        // 执行当前流程控制分支。
        if (candidates.size() <= 1) {
            // 返回当前结果。
            return candidates;
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            log.debug("Reranking {} candidates with query: {}", candidates.size(), query);

            // 处理当前代码结构。
            List<String> texts = candidates.stream()
                    // 处理当前代码结构。
                    .map(Document::getText)
                    // 执行当前语句。
                    .collect(Collectors.toList());

            // 执行当前语句。
            RerankRequest request = new RerankRequest();
            // 执行当前语句。
            request.setModel(ragProperties.getRerankModel());
            // 执行当前语句。
            RerankRequest.Input input = new RerankRequest.Input();
            // 执行当前语句。
            input.setQuery(query);
            // 执行当前语句。
            input.setDocuments(texts);
            // 执行当前语句。
            request.setInput(input);
            // 执行当前语句。
            RerankRequest.Parameters parameters = new RerankRequest.Parameters();
            // 执行当前语句。
            parameters.setTopN(Math.min(topN, candidates.size()));
            // 执行当前语句。
            request.setParameters(parameters);

            // 处理当前代码结构。
            RerankResponse response = restClient.post()
                    // 处理当前代码结构。
                    .contentType(MediaType.APPLICATION_JSON)
                    // 处理当前代码结构。
                    .body(request)
                    // 处理当前代码结构。
                    .retrieve()
                    // 执行当前语句。
                    .body(RerankResponse.class);

            // 执行当前流程控制分支。
            if (response == null || response.getOutput() == null
                    // 处理当前代码结构。
                    || response.getOutput().getResults() == null) {
                // 执行当前语句。
                log.warn("Rerank API returned empty response, using original order");
                // 返回当前结果。
                return candidates.stream().limit(topN).collect(Collectors.toList());
            // 结束当前代码块。
            }

            // 执行当前语句。
            List<RerankResponse.Result> results = response.getOutput().getResults();
            // 执行当前语句。
            results.sort(Comparator.comparingDouble(RerankResponse.Result::getRelevanceScore).reversed());

            // 执行当前语句。
            List<Document> reranked = new ArrayList<>();
            // 执行当前流程控制分支。
            for (RerankResponse.Result result : results) {
                // 执行当前语句。
                int index = result.getIndex();
                // 执行当前流程控制分支。
                if (index >= 0 && index < candidates.size()) {
                    // 执行当前语句。
                    reranked.add(candidates.get(index));
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }

            // 执行当前语句。
            log.info("Reranking complete, returned {} documents", reranked.size());
            // 返回当前结果。
            return reranked;

        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("Rerank failed, using original order: {}", e.getMessage());
            // 返回当前结果。
            return candidates.stream().limit(topN).collect(Collectors.toList());
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    // 应用当前注解。
    @Data
    // 声明当前类型。
    static class RerankRequest {
        // 执行当前语句。
        private String model;
        // 执行当前语句。
        private Input input;
        // 执行当前语句。
        private Parameters parameters;

        // 应用当前注解。
        @Data
        // 声明当前类型。
        static class Input {
            // 执行当前语句。
            private String query;
            // 执行当前语句。
            private List<String> documents;
        // 结束当前代码块。
        }

        // 应用当前注解。
        @Data
        // 声明当前类型。
        static class Parameters {
            // 应用当前注解。
            @JsonProperty("top_n")
            // 执行当前语句。
            private int topN;
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    // 应用当前注解。
    @Data
    // 应用当前注解。
    @JsonIgnoreProperties(ignoreUnknown = true)
    // 声明当前类型。
    static class RerankResponse {
        // 执行当前语句。
        private Output output;

        // 应用当前注解。
        @Data
        // 应用当前注解。
        @JsonIgnoreProperties(ignoreUnknown = true)
        // 声明当前类型。
        static class Output {
            // 执行当前语句。
            private List<Result> results;
        // 结束当前代码块。
        }

        // 应用当前注解。
        @Data
        // 应用当前注解。
        @JsonIgnoreProperties(ignoreUnknown = true)
        // 声明当前类型。
        static class Result {
            // 执行当前语句。
            private int index;
            // 应用当前注解。
            @JsonProperty("relevance_score")
            // 执行当前语句。
            private double relevanceScore;
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }
// 结束当前代码块。
}
