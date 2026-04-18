// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义重排服务，负责调用外部接口优化候选文档顺序。
public class RerankService {

    // 声明ragproperties变量，供后续流程使用。
    private final RagProperties ragProperties;
    // 声明接口key变量，供后续流程使用。
    private final String apiKey;
    // 声明restclient变量，供后续流程使用。
    private final RestClient restClient;

    // 定义重排url常量。
    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-reranking/generation";

    /**
     * 处理重排service相关逻辑。
     * @param ragProperties ragproperties参数。
     * @param apiKey 接口key参数。
     */
    public RerankService(
            RagProperties ragProperties,
            @Value("${spring.ai.dashscope.api-key:${spring.ai.openai.api-key:}}") String apiKey) {
        // 计算并保存this.ragproperties结果。
        this.ragProperties = ragProperties;
        // 计算并保存this.apikey结果。
        this.apiKey = apiKey;

        // 创建请求factory对象。
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 更新connecttimeout字段。
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        // 更新readtimeout字段。
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        // 围绕restclientrest补充当前业务语句。
        this.restClient = RestClient.builder()
                // 设置baseurl字段的取值。
                .baseUrl(RERANK_URL)
                // 设置请求factory字段的取值。
                .requestFactory(requestFactory)
                // 设置defaultheader字段的取值。
                .defaultHeader("Authorization", "Bearer " + apiKey)
                // 设置defaultheader字段的取值。
                .defaultHeader("Content-Type", "application/json")
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 调用重排接口，对候选文档按相关性重新排序。
     * @param query 查询参数。
     * @param candidates candidates参数。
     * @param topN topn参数。
     * @return 列表形式的处理结果。
     */
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        // 根据条件判断当前分支是否执行。
        if (candidates.size() <= 1) {
            // 返回candidates。
            return candidates;
        }

        // 进入异常保护块执行关键逻辑。
        try {
            // 记录当前流程的运行日志。
            log.debug("Reranking {} candidates with query: {}", candidates.size(), query);

            // 围绕textscandidatesstream补充当前业务语句。
            List<String> texts = candidates.stream()
                    // 设置map字段的取值。
                    .map(Document::getText)
                    // 设置collect字段的取值。
                    .collect(Collectors.toList());

            // 创建请求对象。
            RerankRequest request = new RerankRequest();
            // 更新模型字段。
            request.setModel(ragProperties.getRerankModel());
            // 创建输入对象。
            RerankRequest.Input input = new RerankRequest.Input();
            // 更新查询字段。
            input.setQuery(query);
            // 更新文档字段。
            input.setDocuments(texts);
            // 更新输入字段。
            request.setInput(input);
            // 创建parameters对象。
            RerankRequest.Parameters parameters = new RerankRequest.Parameters();
            // 更新topn字段。
            parameters.setTopN(Math.min(topN, candidates.size()));
            // 更新parameters字段。
            request.setParameters(parameters);

            // 围绕重排响应响应补充当前业务语句。
            RerankResponse response = restClient.post()
                    // 设置内容type字段的取值。
                    .contentType(MediaType.APPLICATION_JSON)
                    // 设置body字段的取值。
                    .body(request)
                    // 设置retrieve字段的取值。
                    .retrieve()
                    // 定义当前类型。
                    .body(RerankResponse.class);

            // 根据条件判断当前分支是否执行。
            if (response == null || response.getOutput() == null
                    || response.getOutput().getResults() == null) {
                // 记录当前流程的运行日志。
                log.warn("Rerank API returned empty response, using original order");
                // 返回 `stream` 的处理结果。
                return candidates.stream().limit(topN).collect(Collectors.toList());
            }

            // 计算并保存结果结果。
            List<RerankResponse.Result> results = response.getOutput().getResults();
            // 调用 `sort` 完成当前步骤。
            results.sort(Comparator.comparingDouble(RerankResponse.Result::getRelevanceScore).reversed());

            // 创建reranked对象。
            List<Document> reranked = new ArrayList<>();
            // 遍历当前集合或区间中的元素。
            for (RerankResponse.Result result : results) {
                // 计算并保存index结果。
                int index = result.getIndex();
                // 根据条件判断当前分支是否执行。
                if (index >= 0 && index < candidates.size()) {
                    // 向当前集合中追加元素。
                    reranked.add(candidates.get(index));
                }
            }

            // 记录当前流程的运行日志。
            log.info("Reranking complete, returned {} documents", reranked.size());
            // 返回reranked。
            return reranked;

        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("Rerank failed, using original order: {}", e.getMessage());
            // 返回 `stream` 的处理结果。
            return candidates.stream().limit(topN).collect(Collectors.toList());
        }
    }

    // 让 Lombok 为当前类型生成常用访问方法。
    @Data
    // 定义 `RerankRequest` 类型。
    static class RerankRequest {
        // 声明模型变量，供后续流程使用。
        private String model;
        // 声明输入变量，供后续流程使用。
        private Input input;
        // 声明parameters变量，供后续流程使用。
        private Parameters parameters;

        // 让 Lombok 为当前类型生成常用访问方法。
        @Data
        // 定义 `Input` 类型。
        static class Input {
            // 声明查询变量，供后续流程使用。
            private String query;
            // 声明文档变量，供后续流程使用。
            private List<String> documents;
        }

        // 让 Lombok 为当前类型生成常用访问方法。
        @Data
        // 定义 `Parameters` 类型。
        static class Parameters {
            // 声明 JSON 字段与 Java 字段的映射名称。
            @JsonProperty("top_n")
            // 声明topn变量，供后续流程使用。
            private int topN;
        }
    }

    // 让 Lombok 为当前类型生成常用访问方法。
    @Data
    // 声明反序列化时忽略未使用字段。
    @JsonIgnoreProperties(ignoreUnknown = true)
    // 定义 `RerankResponse` 类型。
    static class RerankResponse {
        // 声明输出变量，供后续流程使用。
        private Output output;

        // 让 Lombok 为当前类型生成常用访问方法。
        @Data
        // 声明反序列化时忽略未使用字段。
        @JsonIgnoreProperties(ignoreUnknown = true)
        // 定义 `Output` 类型。
        static class Output {
            // 声明结果变量，供后续流程使用。
            private List<Result> results;
        }

        // 让 Lombok 为当前类型生成常用访问方法。
        @Data
        // 声明反序列化时忽略未使用字段。
        @JsonIgnoreProperties(ignoreUnknown = true)
        // 定义 `Result` 类型。
        static class Result {
            // 声明index变量，供后续流程使用。
            private int index;
            // 声明 JSON 字段与 Java 字段的映射名称。
            @JsonProperty("relevance_score")
            // 声明relevancescore变量，供后续流程使用。
            private double relevanceScore;
        }
    }
}
