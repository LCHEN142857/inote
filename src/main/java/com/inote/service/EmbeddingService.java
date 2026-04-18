// 声明当前源文件所属包。
package com.inote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义向量服务，负责文本向量化和相似度检索。
public class EmbeddingService {

    // 声明向量模型变量，供后续流程使用。
    private final EmbeddingModel embeddingModel;
    // 声明向量store变量，供后续流程使用。
    private final VectorStore vectorStore;

    /**
     * 将文档分块写入向量库，供后续语义检索使用。
     * @param chunks 分块参数。
     * @param metadata 元数据参数。
     */
    public void embedAndStore(List<String> chunks, Map<String, Object> metadata) {
        // 根据条件判断当前分支是否执行。
        if (chunks == null || chunks.isEmpty()) {
            // 记录当前流程的运行日志。
            log.warn("No chunks to embed");
            // 继续补全当前链式调用或多行表达式。
            return;
        }

        // 记录当前流程的运行日志。
        log.info("Embedding {} chunks", chunks.size());

        // 围绕文档文档分块补充当前业务语句。
        List<Document> documents = chunks.stream()
                // 设置map字段的取值。
                .map(chunk -> {
                    // 创建doc元数据对象。
                    Map<String, Object> docMetadata = new HashMap<>(metadata);
                    // 写入当前映射中的键值对。
                    docMetadata.put("chunk_size", chunk.length());
                    // 返回 `Document` 的处理结果。
                    return new Document(chunk, docMetadata);
                })
                // 设置tolist字段的取值。
                .toList();

        // 向当前集合中追加元素。
        vectorStore.add(documents);
        // 记录当前流程的运行日志。
        log.info("Successfully stored {} documents to vector store", documents.size());
    }

    /**
     * 按查询内容检索相似文档。
     * @param query 查询参数。
     * @param topK topk参数。
     * @return 列表形式的处理结果。
     */
    public List<Document> searchSimilarDocuments(String query, int topK) {
        // 返回 `searchSimilarDocuments` 的处理结果。
        return searchSimilarDocuments(query, topK, 0.0, null);
    }

    /**
     * 按查询内容检索相似文档。
     * @param query 查询参数。
     * @param topK topk参数。
     * @param similarityThreshold similaritythreshold参数。
     * @return 列表形式的处理结果。
     */
    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold) {
        // 返回 `searchSimilarDocuments` 的处理结果。
        return searchSimilarDocuments(query, topK, similarityThreshold, null);
    }

    /**
     * 按查询内容检索相似文档。
     * @param query 查询参数。
     * @param topK topk参数。
     * @param similarityThreshold similaritythreshold参数。
     * @param ownerId 所属用户id参数。
     * @return 列表形式的处理结果。
     */
    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold, String ownerId) {
        // 围绕logdebugsearching补充当前业务语句。
        log.debug("Searching similar documents for query: {}, topK: {}, threshold: {}",
                // 围绕查询topk补充当前业务语句。
                query, topK, similarityThreshold);

        // 围绕search请求builder补充当前业务语句。
        SearchRequest.Builder builder = SearchRequest.builder()
                // 设置查询字段的取值。
                .query(query)
                // 设置topk字段的取值。
                .topK(topK);

        // 根据条件判断当前分支是否执行。
        if (similarityThreshold > 0.0) {
            // 调用 `similarityThreshold` 完成当前步骤。
            builder.similarityThreshold(similarityThreshold);
        }

        // 根据条件判断当前分支是否执行。
        if (StringUtils.hasText(ownerId)) {
            // 调用 `filterExpression` 完成当前步骤。
            builder.filterExpression("owner_id == '" + ownerId + "'");
        }

        // 计算并保存search请求结果。
        SearchRequest searchRequest = builder.build();
        // 计算并保存结果结果。
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        // 记录当前流程的运行日志。
        log.debug("Found {} similar documents", results.size());
        // 返回结果。
        return results;
    }

    /**
     * 对单段文本生成向量表示。
     * @param text text参数。
     * @return float[]结果。
     */
    public float[] embed(String text) {
        // 构造响应固定列表。
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        // 根据条件判断当前分支是否执行。
        if (response.getResults() != null && !response.getResults().isEmpty()) {
            // 返回 `getResults` 的处理结果。
            return response.getResults().get(0).getOutput();
        }
        // 返回newfloat[0]。
        return new float[0];
    }
}
