// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义检索管线服务，负责串联改写、拆分、检索和重排流程。
public class RetrievalPipelineService {

    // 声明ragproperties变量，供后续流程使用。
    private final RagProperties ragProperties;
    // 声明查询改写service变量，供后续流程使用。
    private final QueryRewriteService queryRewriteService;
    // 声明查询拆分service变量，供后续流程使用。
    private final QueryDecompositionService queryDecompositionService;
    // 声明hybrid检索service变量，供后续流程使用。
    private final HybridRetrievalService hybridRetrievalService;
    // 声明重排service变量，供后续流程使用。
    private final RerankService rerankService;

    /**
     * 执行完整检索流程，返回最终候选文档。
     * @param originalQuery original查询参数。
     * @return 检索结果结果。
     */
    public RetrievalResult retrieve(String originalQuery) {
        // 记录当前流程的运行日志。
        log.info("Starting retrieval pipeline for query: {}", originalQuery);

        // 计算并保存search查询结果。
        String searchQuery = originalQuery;
        // 根据条件判断当前分支是否执行。
        if (ragProperties.isQueryRewriteEnabled()) {
            // 计算并保存search查询结果。
            searchQuery = queryRewriteService.rewrite(originalQuery);
        }

        // 声明queries变量，供后续流程使用。
        List<String> queries;
        // 根据条件判断当前分支是否执行。
        if (ragProperties.isMultiQueryEnabled()) {
            // 计算并保存queries结果。
            queries = queryDecompositionService.decompose(searchQuery);
        } else {
            // 构造queries固定列表。
            queries = List.of(searchQuery);
        }

        // 创建allcandidates对象。
        List<Document> allCandidates = new ArrayList<>();
        // 创建seenids对象。
        Set<String> seenIds = new HashSet<>();
        // 遍历当前集合或区间中的元素。
        for (String query : queries) {
            // 执行检索流程并获取候选文档。
            List<Document> results = hybridRetrievalService.retrieve(query);
            // 遍历当前集合或区间中的元素。
            for (Document doc : results) {
                // 根据条件判断当前分支是否执行。
                if (seenIds.add(doc.getId())) {
                    // 向当前集合中追加元素。
                    allCandidates.add(doc);
                }
            }
        }

        // 声明finaldocs变量，供后续流程使用。
        List<Document> finalDocs;
        // 根据条件判断当前分支是否执行。
        if (ragProperties.isRerankEnabled() && allCandidates.size() > 1) {
            // 围绕docs重排service补充当前业务语句。
            finalDocs = rerankService.rerank(
                    // 调用 `getRerankTopN` 完成当前步骤。
                    originalQuery, allCandidates, ragProperties.getRerankTopN());
        } else {
            // 围绕docsallcandidates补充当前业务语句。
            finalDocs = allCandidates.stream()
                    // 设置limit字段的取值。
                    .limit(ragProperties.getFinalTopK())
                    // 设置collect字段的取值。
                    .collect(Collectors.toList());
        }

        // 记录当前流程的运行日志。
        log.info("Retrieval pipeline completed, {} documents returned", finalDocs.size());
        // 返回 `RetrievalResult` 的处理结果。
        return new RetrievalResult(originalQuery, searchQuery, finalDocs);
    }
}
