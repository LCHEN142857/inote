// 声明包路径，服务层
package com.inote.service;

// 导入 Lombok 构造函数注解
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 Spring AI 文档类（与 Milvus 交互的文档对象）
import org.springframework.ai.document.Document;
// 导入 Spring AI 嵌入模型接口
import org.springframework.ai.embedding.EmbeddingModel;
// 导入 Spring AI 嵌入响应类
import org.springframework.ai.embedding.EmbeddingResponse;
// 导入 Spring AI 向量搜索请求类
import org.springframework.ai.vectorstore.SearchRequest;
// 导入 Spring AI 向量存储接口
import org.springframework.ai.vectorstore.VectorStore;
// 导入 Spring 服务注解
import org.springframework.stereotype.Service;

// 导入 HashMap 集合
import java.util.HashMap;
// 导入 List 集合接口
import java.util.List;
// 导入 Map 集合接口
import java.util.Map;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 服务层组件
@Service
// 为所有 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
// 嵌入服务类，封装文本向量化和向量数据库的读写操作
public class EmbeddingService {

    // 注入嵌入模型（通过 DashScope text-embedding-v2 模型将文本转为向量）
    private final EmbeddingModel embeddingModel;
    // 注入向量存储（Milvus 向量数据库实例）
    private final VectorStore vectorStore;

    /**
     * 将文本块向量化并存入向量数据库
     * @param chunks 文本块列表（文档分块后的结果）
     * @param metadata 元数据（文件名、URL 等），每个文本块都会携带这些元数据
     */
    public void embedAndStore(List<String> chunks, Map<String, Object> metadata) {
        // 检查文本块列表是否为空
        if (chunks == null || chunks.isEmpty()) {
            // 记录警告日志
            log.warn("No chunks to embed");
            // 空列表直接返回
            return;
        }

        // 记录待向量化的文本块数量
        log.info("Embedding {} chunks", chunks.size());

        // 将文本块列表转为 Spring AI Document 对象列表
        List<Document> documents = chunks.stream()
                // 将每个文本块映射为 Document 对象
                .map(chunk -> {
                    // 复制公共元数据，避免修改原始 Map
                    Map<String, Object> docMetadata = new HashMap<>(metadata);
                    // 添加当前文本块的字符数到元数据
                    docMetadata.put("chunk_size", chunk.length());
                    // 创建 Document 对象，包含文本内容和元数据
                    return new Document(chunk, docMetadata);
                })
                // 收集为不可变 List
                .toList();

        // 将文档列表添加到向量存储（Milvus），内部会自动调用嵌入模型进行向量化
        vectorStore.add(documents);
        // 记录存储成功的日志
        log.info("Successfully stored {} documents to vector store", documents.size());
    }

    /**
     * 根据查询文本检索最相似的文档
     * @param query 用户输入的查询文本
     * @param topK 返回最相似的文档数量
     * @return 按相似度排序的文档列表
     */
    public List<Document> searchSimilarDocuments(String query, int topK) {
        // 记录搜索日志
        log.debug("Searching similar documents for query: {}", query);

        // 构建向量搜索请求
        SearchRequest searchRequest = SearchRequest.builder()
                // 设置查询文本（内部会自动向量化）
                .query(query)
                // 设置返回的最大文档数量
                .topK(topK)
                // 构建搜索请求对象
                .build();

        // 在 Milvus 中执行向量相似度搜索
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        // 记录搜索结果数量
        log.debug("Found {} similar documents", results.size());

        // 返回相似文档列表
        return results;
    }

    /**
     * 获取单段文本的嵌入向量
     * @param text 输入文本
     * @return 嵌入向量（浮点数数组）
     */
    public float[] embed(String text) {
        // 调用嵌入模型获取向量化响应
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        // 检查响应中是否有结果
        if (response.getResults() != null && !response.getResults().isEmpty()) {
            // 返回第一个结果的向量数组
            return response.getResults().get(0).getOutput();
        }
        // 如果没有结果，返回空数组
        return new float[0];
    }
}
