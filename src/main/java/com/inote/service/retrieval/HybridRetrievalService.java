// 声明当前源文件的包。
package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import com.inote.security.CurrentUserService;
import com.inote.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class HybridRetrievalService {

    // 声明当前字段。
    private final EmbeddingService embeddingService;
    // 声明当前字段。
    private final BM25SearchService bm25SearchService;
    // 声明当前字段。
    private final RagProperties ragProperties;
    // 声明当前字段。
    private final CurrentUserService currentUserService;

    /**
     * 描述 `retrieve` 操作。
     *
     * @param query 输入参数 `query`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    public List<Document> retrieve(String query) {
        // 执行当前语句。
        int vectorTopK = ragProperties.getVectorTopK();
        // 执行当前语句。
        String ownerId = currentUserService.getCurrentUser().getId();

        // 处理当前代码结构。
        List<Document> vectorResults = embeddingService.searchSimilarDocuments(
                // 执行当前语句。
                query, vectorTopK, ragProperties.getSimilarityThreshold(), ownerId);
        // 执行当前语句。
        log.debug("Vector search returned {} results", vectorResults.size());

        // 执行当前流程控制分支。
        if (!ragProperties.isHybridSearchEnabled()) {
            // 返回当前结果。
            return vectorResults;
        // 结束当前代码块。
        }

        // 执行当前语句。
        int bm25TopK = ragProperties.getBm25TopK();
        // 执行当前语句。
        List<BM25SearchService.BM25Result> bm25Results;
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            bm25Results = bm25SearchService.search(query, bm25TopK, ownerId);
            // 执行当前语句。
            log.debug("BM25 search returned {} results", bm25Results.size());
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("BM25 search failed, using vector-only results: {}", e.getMessage());
            // 返回当前结果。
            return vectorResults;
        // 结束当前代码块。
        }

        // 返回当前结果。
        return fuseWithRRF(vectorResults, bm25Results);
    // 结束当前代码块。
    }

    /**
     * 描述 `fuseWithRRF` 操作。
     *
     * @param vectorResults 输入参数 `vectorResults`。
     * @param bm25Results 输入参数 `bm25Results`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    private List<Document> fuseWithRRF(
            // 处理当前代码结构。
            List<Document> vectorResults,
            // 处理当前代码结构。
            List<BM25SearchService.BM25Result> bm25Results) {

        // 执行当前语句。
        double k = ragProperties.getRrfK();
        // 执行当前语句。
        Map<String, Double> rrfScores = new HashMap<>();
        // 执行当前语句。
        Map<String, Document> docMap = new LinkedHashMap<>();

        // 执行当前流程控制分支。
        for (int i = 0; i < vectorResults.size(); i++) {
            // 执行当前语句。
            Document doc = vectorResults.get(i);
            // 执行当前语句。
            String id = doc.getId();
            // 执行当前语句。
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            // 执行当前语句。
            docMap.putIfAbsent(id, doc);
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        for (int i = 0; i < bm25Results.size(); i++) {
            // 执行当前语句。
            BM25SearchService.BM25Result bm25 = bm25Results.get(i);
            // 执行当前语句。
            String id = bm25.documentId();
            // 执行当前语句。
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            // 执行当前语句。
            docMap.putIfAbsent(id, new Document(id, bm25.content(), bm25.metadata()));
        // 结束当前代码块。
        }

        // 执行当前语句。
        List<Document> fused = new ArrayList<>(docMap.values());
        // 处理当前代码结构。
        fused.sort((a, b) -> Double.compare(
                // 处理当前代码结构。
                rrfScores.getOrDefault(b.getId(), 0.0),
                // 执行当前语句。
                rrfScores.getOrDefault(a.getId(), 0.0)));

        // 执行当前语句。
        log.debug("RRF fusion produced {} unique documents", fused.size());
        // 返回当前结果。
        return fused;
    // 结束当前代码块。
    }
// 结束当前代码块。
}
