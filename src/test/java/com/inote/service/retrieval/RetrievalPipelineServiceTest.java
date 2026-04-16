// 声明测试类所在包。
package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 标记当前类使用 Mockito 扩展。
@ExtendWith(MockitoExtension.class)
class RetrievalPipelineServiceTest {

    // 声明 RAG 配置对象。
    private RagProperties ragProperties;

    // 声明查询改写服务模拟对象。
    @Mock
    private QueryRewriteService queryRewriteService;

    // 声明查询拆分服务模拟对象。
    @Mock
    private QueryDecompositionService queryDecompositionService;

    // 声明混合检索服务模拟对象。
    @Mock
    private HybridRetrievalService hybridRetrievalService;

    // 声明重排服务模拟对象。
    @Mock
    private RerankService rerankService;

    // 声明被测服务实例。
    @InjectMocks
    private RetrievalPipelineService retrievalPipelineService;

    /**
     * 初始化检索流水线测试环境。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当反射写入字段失败时抛出异常。
     */
    @BeforeEach
    void setUp() throws Exception {
        // 创建 RAG 配置对象。
        ragProperties = new RagProperties();
        // 关闭查询改写能力。
        ragProperties.setQueryRewriteEnabled(false);
        // 关闭多查询能力。
        ragProperties.setMultiQueryEnabled(false);
        // 关闭重排能力。
        ragProperties.setRerankEnabled(false);
        // 设置最终返回文档数量。
        ragProperties.setFinalTopK(2);
        // 通过反射写入配置字段。
        org.springframework.test.util.ReflectionTestUtils.setField(retrievalPipelineService, "ragProperties", ragProperties);
    }

    /**
     * 验证默认路径会直接返回混合检索结果并遵循数量限制。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void retrieveUsesShortestPathWhenOptionalStagesAreDisabled() throws Exception {
        // 构造三个候选文档。
        Document firstDocument = new Document("doc-1", Map.of("file_name", "a.txt"));
        // 构造第二个候选文档。
        Document secondDocument = new Document("doc-2", Map.of("file_name", "b.txt"));
        // 构造第三个候选文档。
        Document thirdDocument = new Document("doc-3", Map.of("file_name", "c.txt"));
        // 模拟混合检索返回候选列表。
        when(hybridRetrievalService.retrieve("question")).thenReturn(List.of(firstDocument, secondDocument, thirdDocument));
        // 调用检索方法。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 断言搜索查询未被改写。
        assertThat(result.searchQuery()).isEqualTo("question");
        // 断言返回文档数量遵循 finalTopK。
        assertThat(result.documents()).hasSize(2);
        // 断言未调用改写服务。
        verify(queryRewriteService, never()).rewrite("question");
        // 断言未调用拆分服务。
        verify(queryDecompositionService, never()).decompose("question");
        // 断言未调用重排服务。
        verify(rerankService, never()).rerank(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    /**
     * 验证开启改写与多查询时会去重合并候选结果。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void retrieveRewritesQueryAndDeduplicatesAcrossSubQueries() throws Exception {
        // 开启查询改写。
        ragProperties.setQueryRewriteEnabled(true);
        // 开启多查询。
        ragProperties.setMultiQueryEnabled(true);
        // 构造共享主键文档。
        Document sharedDocument = Document.builder().id("shared-id").text("shared").metadata(Map.of("file_name", "shared.txt")).build();
        // 构造唯一文档。
        Document uniqueDocument = Document.builder().id("unique-id").text("unique").metadata(Map.of("file_name", "unique.txt")).build();
        // 模拟改写结果。
        when(queryRewriteService.rewrite("question")).thenReturn("rewritten question");
        // 模拟拆分结果。
        when(queryDecompositionService.decompose("rewritten question")).thenReturn(List.of("query-a", "query-b"));
        // 模拟第一路检索结果。
        when(hybridRetrievalService.retrieve("query-a")).thenReturn(List.of(sharedDocument));
        // 模拟第二路检索结果。
        when(hybridRetrievalService.retrieve("query-b")).thenReturn(List.of(sharedDocument, uniqueDocument));
        // 调用检索方法。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 断言搜索查询采用改写结果。
        assertThat(result.searchQuery()).isEqualTo("rewritten question");
        // 断言结果经过去重后仅保留两个文档。
        assertThat(result.documents()).hasSize(2);
        // 断言去重后仍包含共享文档。
        assertThat(result.documents()).extracting(Document::getId).containsExactly("shared-id", "unique-id");
    }
}
