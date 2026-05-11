package com.inote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean queryRewriteEnabled = false;

    private boolean multiQueryEnabled = false;
    private int maxSubQueries = 3;

    private boolean hybridSearchEnabled = false;
    private int vectorTopK = 10;
    private int bm25TopK = 10;
    private double similarityThreshold = 0.5;
    private double rrfK = 60.0;

    private boolean rerankEnabled = false;
    private String rerankModel = "gte-rerank";
    private int rerankTopN = 3;

    private int finalTopK = 3;
}
