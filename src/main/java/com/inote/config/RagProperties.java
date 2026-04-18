// 声明当前源文件所属包。
package com.inote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 让 Lombok 为当前类型生成常用访问方法。
@Data
// 应用 `ConfigurationProperties` 注解声明当前行为。
@ConfigurationProperties(prefix = "rag")
// 定义 RAG 配置属性，负责承载检索链路参数。
public class RagProperties {

    // 计算并保存查询改写enabled结果。
    private boolean queryRewriteEnabled = false;

    // 计算并保存multi查询enabled结果。
    private boolean multiQueryEnabled = false;
    // 计算并保存max子queries结果。
    private int maxSubQueries = 3;

    // 计算并保存hybridsearchenabled结果。
    private boolean hybridSearchEnabled = false;
    // 计算并保存向量topk结果。
    private int vectorTopK = 10;
    // 计算并保存BM25topk结果。
    private int bm25TopK = 10;
    // 计算并保存similaritythreshold结果。
    private double similarityThreshold = 0.5;
    // 计算并保存rrfk结果。
    private double rrfK = 60.0;

    // 计算并保存重排enabled结果。
    private boolean rerankEnabled = false;
    // 计算并保存重排模型结果。
    private String rerankModel = "gte-rerank";
    // 计算并保存重排topn结果。
    private int rerankTopN = 3;

    // 计算并保存finaltopk结果。
    private int finalTopK = 3;
}
