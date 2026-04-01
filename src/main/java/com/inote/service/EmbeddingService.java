package com.inote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    /**
     * 将文本块存入向量数据库
     * @param chunks 文本块列表
     * @param metadata 元数据（文件名、URL等）
     */
    public void embedAndStore(List<String> chunks, Map<String, Object> metadata) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks to embed");
            return;
        }

        log.info("Embedding {} chunks", chunks.size());
        
        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> docMetadata = new HashMap<>(metadata);
                    docMetadata.put("chunk_size", chunk.length());
                    return new Document(chunk, docMetadata);
                })
                .toList();

        vectorStore.add(documents);
        log.info("Successfully stored {} documents to vector store", documents.size());
    }

    /**
     * 根据查询文本检索相似文档
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    public List<Document> searchSimilarDocuments(String query, int topK) {
        log.debug("Searching similar documents for query: {}", query);
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.debug("Found {} similar documents", results.size());
        
        return results;
    }

    /**
     * 获取文本的嵌入向量
     * @param text 输入文本
     * @return 嵌入向量
     */
    public float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        if (response.getResults() != null && !response.getResults().isEmpty()) {
            return response.getResults().get(0).getOutput();
        }
        return new float[0];
    }
}
