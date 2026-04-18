// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义混合检索服务，负责融合向量检索和 BM25 检索结果。
public class HybridRetrievalService {

    // 声明向量service变量，供后续流程使用。
    private final EmbeddingService embeddingService;
    // 声明BM25searchservice变量，供后续流程使用。
    private final BM25SearchService bm25SearchService;
    // 声明ragproperties变量，供后续流程使用。
    private final RagProperties ragProperties;
    // 声明当前用户service变量，供后续流程使用。
    private final CurrentUserService currentUserService;

    /**
     * 执行向量检索，并在开启混合检索时融合 BM25 结果。
     * @param query 查询参数。
     * @return 列表形式的处理结果。
     */
    public List<Document> retrieve(String query) {
        // 计算并保存向量topk结果。
        int vectorTopK = ragProperties.getVectorTopK();
        // 获取当前登录用户。
        String ownerId = currentUserService.getCurrentUser().getId();

        // 围绕文档向量结果补充当前业务语句。
        List<Document> vectorResults = embeddingService.searchSimilarDocuments(
                // 调用 `getSimilarityThreshold` 完成当前步骤。
                query, vectorTopK, ragProperties.getSimilarityThreshold(), ownerId);
        // 记录当前流程的运行日志。
        log.debug("Vector search returned {} results", vectorResults.size());

        // 根据条件判断当前分支是否执行。
        if (!ragProperties.isHybridSearchEnabled()) {
            // 返回向量结果。
            return vectorResults;
        }

        // 计算并保存BM25topk结果。
        int bm25TopK = ragProperties.getBm25TopK();
        // 围绕BM25searchservice补充当前业务语句。
        List<BM25SearchService.BM25Result> bm25Results;
        // 进入异常保护块执行关键逻辑。
        try {
            // 计算并保存BM25结果结果。
            bm25Results = bm25SearchService.search(query, bm25TopK, ownerId);
            // 记录当前流程的运行日志。
            log.debug("BM25 search returned {} results", bm25Results.size());
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("BM25 search failed, using vector-only results: {}", e.getMessage());
            // 返回向量结果。
            return vectorResults;
        }

        // 返回 `fuseWithRRF` 的处理结果。
        return fuseWithRRF(vectorResults, bm25Results);
    }

    /**
     * 使用倒数排序融合算法整合两路检索结果。
     * @param vectorResults 向量结果参数。
     * @param bm25Results BM25结果参数。
     * @return 列表形式的处理结果。
     */
    private List<Document> fuseWithRRF(
            List<Document> vectorResults,
            List<BM25SearchService.BM25Result> bm25Results) {

        // 计算并保存k结果。
        double k = ragProperties.getRrfK();
        // 创建rrfscores对象。
        Map<String, Double> rrfScores = new HashMap<>();
        // 创建docmap对象。
        Map<String, Document> docMap = new LinkedHashMap<>();

        // 遍历当前集合或区间中的元素。
        for (int i = 0; i < vectorResults.size(); i++) {
            // 计算并保存doc结果。
            Document doc = vectorResults.get(i);
            // 计算并保存id结果。
            String id = doc.getId();
            // 调用 `merge` 完成当前步骤。
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            // 调用 `putIfAbsent` 完成当前步骤。
            docMap.putIfAbsent(id, doc);
        }

        // 遍历当前集合或区间中的元素。
        for (int i = 0; i < bm25Results.size(); i++) {
            // 计算并保存BM25结果。
            BM25SearchService.BM25Result bm25 = bm25Results.get(i);
            // 计算并保存id结果。
            String id = bm25.documentId();
            // 调用 `merge` 完成当前步骤。
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            // 调用 `putIfAbsent` 完成当前步骤。
            docMap.putIfAbsent(id, new Document(id, bm25.content(), bm25.metadata()));
        }

        // 创建fused对象。
        List<Document> fused = new ArrayList<>(docMap.values());
        // 围绕fusedsorta补充当前业务语句。
        fused.sort((a, b) -> Double.compare(
                // 围绕rrfscoresget补充当前业务语句。
                rrfScores.getOrDefault(b.getId(), 0.0),
                // 调用 `getOrDefault` 完成当前步骤。
                rrfScores.getOrDefault(a.getId(), 0.0)));

        // 记录当前流程的运行日志。
        log.debug("RRF fusion produced {} unique documents", fused.size());
        // 返回fused。
        return fused;
    }
}
