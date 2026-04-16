// 声明当前源文件的包。
package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class RetrievalPipelineService {

    // 声明当前字段。
    private final RagProperties ragProperties;
    // 声明当前字段。
    private final QueryRewriteService queryRewriteService;
    // 声明当前字段。
    private final QueryDecompositionService queryDecompositionService;
    // 声明当前字段。
    private final HybridRetrievalService hybridRetrievalService;
    // 声明当前字段。
    private final RerankService rerankService;

    /**
     * 描述 `retrieve` 操作。
     *
     * @param originalQuery 输入参数 `originalQuery`。
     * @return 类型为 `RetrievalResult` 的返回值。
     */
    // 处理当前代码结构。
    public RetrievalResult retrieve(String originalQuery) {
        // 执行当前语句。
        log.info("Starting retrieval pipeline for query: {}", originalQuery);

        // 执行当前语句。
        String searchQuery = originalQuery;
        // 执行当前流程控制分支。
        if (ragProperties.isQueryRewriteEnabled()) {
            // 执行当前语句。
            searchQuery = queryRewriteService.rewrite(originalQuery);
        // 结束当前代码块。
        }

        // 执行当前语句。
        List<String> queries;
        // 执行当前流程控制分支。
        if (ragProperties.isMultiQueryEnabled()) {
            // 执行当前语句。
            queries = queryDecompositionService.decompose(searchQuery);
        // 处理当前代码结构。
        } else {
            // 执行当前语句。
            queries = List.of(searchQuery);
        // 结束当前代码块。
        }

        // 执行当前语句。
        List<Document> allCandidates = new ArrayList<>();
        // 执行当前语句。
        Set<String> seenIds = new HashSet<>();
        // 执行当前流程控制分支。
        for (String query : queries) {
            // 执行当前语句。
            List<Document> results = hybridRetrievalService.retrieve(query);
            // 执行当前流程控制分支。
            for (Document doc : results) {
                // 执行当前流程控制分支。
                if (seenIds.add(doc.getId())) {
                    // 执行当前语句。
                    allCandidates.add(doc);
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }

        // 执行当前语句。
        List<Document> finalDocs;
        // 执行当前流程控制分支。
        if (ragProperties.isRerankEnabled() && allCandidates.size() > 1) {
            // 处理当前代码结构。
            finalDocs = rerankService.rerank(
                    // 执行当前语句。
                    originalQuery, allCandidates, ragProperties.getRerankTopN());
        // 处理当前代码结构。
        } else {
            // 处理当前代码结构。
            finalDocs = allCandidates.stream()
                    // 处理当前代码结构。
                    .limit(ragProperties.getFinalTopK())
                    // 执行当前语句。
                    .collect(Collectors.toList());
        // 结束当前代码块。
        }

        // 执行当前语句。
        log.info("Retrieval pipeline completed, {} documents returned", finalDocs.size());
        // 返回当前结果。
        return new RetrievalResult(originalQuery, searchQuery, finalDocs);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
