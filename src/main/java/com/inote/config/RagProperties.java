// 声明当前源文件的包。
package com.inote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 应用当前注解。
@Data
// 应用当前注解。
@ConfigurationProperties(prefix = "rag")
// 声明当前类型。
public class RagProperties {

    // 声明当前字段。
    private boolean queryRewriteEnabled = false;

    // 声明当前字段。
    private boolean multiQueryEnabled = false;
    // 声明当前字段。
    private int maxSubQueries = 3;

    // 声明当前字段。
    private boolean hybridSearchEnabled = false;
    // 声明当前字段。
    private int vectorTopK = 10;
    // 声明当前字段。
    private int bm25TopK = 10;
    // 声明当前字段。
    private double similarityThreshold = 0.5;
    // 声明当前字段。
    private double rrfK = 60.0;

    // 声明当前字段。
    private boolean rerankEnabled = false;
    // 声明当前字段。
    private String rerankModel = "gte-rerank";
    // 声明当前字段。
    private int rerankTopN = 3;

    // 声明当前字段。
    private int finalTopK = 3;
// 结束当前代码块。
}
