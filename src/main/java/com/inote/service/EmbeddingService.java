// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class EmbeddingService {

    // 声明当前字段。
    private final EmbeddingModel embeddingModel;
    // 声明当前字段。
    private final VectorStore vectorStore;

    /**
     * 描述 `embedAndStore` 操作。
     *
     * @param chunks 输入参数 `chunks`。
     * @param metadata 输入参数 `metadata`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public void embedAndStore(List<String> chunks, Map<String, Object> metadata) {
        // 执行当前流程控制分支。
        if (chunks == null || chunks.isEmpty()) {
            // 执行当前语句。
            log.warn("No chunks to embed");
            // 执行当前语句。
            return;
        // 结束当前代码块。
        }

        // 执行当前语句。
        log.info("Embedding {} chunks", chunks.size());

        // 处理当前代码结构。
        List<Document> documents = chunks.stream()
                // 处理当前代码结构。
                .map(chunk -> {
                    // 执行当前语句。
                    Map<String, Object> docMetadata = new HashMap<>(metadata);
                    // 执行当前语句。
                    docMetadata.put("chunk_size", chunk.length());
                    // 返回当前结果。
                    return new Document(chunk, docMetadata);
                // 处理当前代码结构。
                })
                // 执行当前语句。
                .toList();

        // 执行当前语句。
        vectorStore.add(documents);
        // 执行当前语句。
        log.info("Successfully stored {} documents to vector store", documents.size());
    // 结束当前代码块。
    }

    /**
     * 描述 `searchSimilarDocuments` 操作。
     *
     * @param query 输入参数 `query`。
     * @param topK 输入参数 `topK`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    public List<Document> searchSimilarDocuments(String query, int topK) {
        // 返回当前结果。
        return searchSimilarDocuments(query, topK, 0.0, null);
    // 结束当前代码块。
    }

    /**
     * 描述 `searchSimilarDocuments` 操作。
     *
     * @param query 输入参数 `query`。
     * @param topK 输入参数 `topK`。
     * @param similarityThreshold 输入参数 `similarityThreshold`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold) {
        // 返回当前结果。
        return searchSimilarDocuments(query, topK, similarityThreshold, null);
    // 结束当前代码块。
    }

    /**
     * 描述 `searchSimilarDocuments` 操作。
     *
     * @param query 输入参数 `query`。
     * @param topK 输入参数 `topK`。
     * @param similarityThreshold 输入参数 `similarityThreshold`。
     * @param ownerId 输入参数 `ownerId`。
     * @return 类型为 `List<Document>` 的返回值。
     */
    // 处理当前代码结构。
    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold, String ownerId) {
        // 处理当前代码结构。
        log.debug("Searching similar documents for query: {}, topK: {}, threshold: {}",
                // 执行当前语句。
                query, topK, similarityThreshold);

        // 处理当前代码结构。
        SearchRequest.Builder builder = SearchRequest.builder()
                // 处理当前代码结构。
                .query(query)
                // 执行当前语句。
                .topK(topK);

        // 执行当前流程控制分支。
        if (similarityThreshold > 0.0) {
            // 执行当前语句。
            builder.similarityThreshold(similarityThreshold);
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        if (StringUtils.hasText(ownerId)) {
            // 执行当前语句。
            builder.filterExpression("owner_id == '" + ownerId + "'");
        // 结束当前代码块。
        }

        // 执行当前语句。
        SearchRequest searchRequest = builder.build();
        // 执行当前语句。
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        // 执行当前语句。
        log.debug("Found {} similar documents", results.size());
        // 返回当前结果。
        return results;
    // 结束当前代码块。
    }

    /**
     * 描述 `embed` 操作。
     *
     * @param text 输入参数 `text`。
     * @return 类型为 `float[]` 的返回值。
     */
    // 处理当前代码结构。
    public float[] embed(String text) {
        // 执行当前语句。
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        // 执行当前流程控制分支。
        if (response.getResults() != null && !response.getResults().isEmpty()) {
            // 返回当前结果。
            return response.getResults().get(0).getOutput();
        // 结束当前代码块。
        }
        // 返回当前结果。
        return new float[0];
    // 结束当前代码块。
    }
// 结束当前代码块。
}
