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

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final EmbeddingService embeddingService;
    private final BM25SearchService bm25SearchService;
    private final RagProperties ragProperties;
    private final CurrentUserService currentUserService;

    public List<Document> retrieve(String query) {
        int vectorTopK = ragProperties.getVectorTopK();
        String ownerId = currentUserService.getCurrentUser().getId();

        // 向量检索
        List<Document> vectorResults = embeddingService.searchSimilarDocuments(
                query, vectorTopK, ragProperties.getSimilarityThreshold(), ownerId);
        log.debug("Vector search returned {} results", vectorResults.size());

        if (!ragProperties.isHybridSearchEnabled()) {
            return vectorResults;
        }

        // BM25 检索
        int bm25TopK = ragProperties.getBm25TopK();
        List<BM25SearchService.BM25Result> bm25Results;
        try {
            bm25Results = bm25SearchService.search(query, bm25TopK, ownerId);
            log.debug("BM25 search returned {} results", bm25Results.size());
        } catch (Exception e) {
            log.warn("BM25 search failed, using vector-only results: {}", e.getMessage());
            return vectorResults;
        }

        // RRF 融合
        return fuseWithRRF(vectorResults, bm25Results);
    }

    private List<Document> fuseWithRRF(
            List<Document> vectorResults,
            List<BM25SearchService.BM25Result> bm25Results) {

        double k = ragProperties.getRrfK();
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Document> docMap = new LinkedHashMap<>();

        // 向量检索结果的 RRF 分数
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String id = doc.getId();
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, doc);
        }

        // BM25 检索结果的 RRF 分数
        for (int i = 0; i < bm25Results.size(); i++) {
            BM25SearchService.BM25Result bm25 = bm25Results.get(i);
            String id = bm25.documentId();
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            // BM25 结果可能不在向量结果中，需要创建 Document 对象
            docMap.putIfAbsent(id, new Document(id, bm25.content(), bm25.metadata()));
        }

        // 按 RRF 分数降序排列
        List<Document> fused = new ArrayList<>(docMap.values());
        fused.sort((a, b) -> Double.compare(
                rrfScores.getOrDefault(b.getId(), 0.0),
                rrfScores.getOrDefault(a.getId(), 0.0)));

        log.debug("RRF fusion produced {} unique documents", fused.size());
        return fused;
    }
}
