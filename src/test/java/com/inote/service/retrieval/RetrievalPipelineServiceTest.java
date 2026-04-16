// 声明当前源文件的包。
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

// 应用当前注解。
@ExtendWith(MockitoExtension.class)
// 声明当前类型。
class RetrievalPipelineServiceTest {

    // 声明当前字段。
    private RagProperties ragProperties;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private QueryRewriteService queryRewriteService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private QueryDecompositionService queryDecompositionService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private HybridRetrievalService hybridRetrievalService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private RerankService rerankService;

    // 应用当前注解。
    @InjectMocks
    // 声明当前字段。
    private RetrievalPipelineService retrievalPipelineService;

    /**
     * 描述 `setUp` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @BeforeEach
    // 处理当前代码结构。
    void setUp() throws Exception {
        // 执行当前语句。
        ragProperties = new RagProperties();
        // 执行当前语句。
        ragProperties.setQueryRewriteEnabled(false);
        // 执行当前语句。
        ragProperties.setMultiQueryEnabled(false);
        // 执行当前语句。
        ragProperties.setRerankEnabled(false);
        // 执行当前语句。
        ragProperties.setFinalTopK(2);
        // 执行当前语句。
        org.springframework.test.util.ReflectionTestUtils.setField(retrievalPipelineService, "ragProperties", ragProperties);
    // 结束当前代码块。
    }

    /**
     * 描述 `retrieveUsesShortestPathWhenOptionalStagesAreDisabled` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void retrieveUsesShortestPathWhenOptionalStagesAreDisabled() throws Exception {
        // 执行当前语句。
        Document firstDocument = new Document("doc-1", Map.of("file_name", "a.txt"));
        // 执行当前语句。
        Document secondDocument = new Document("doc-2", Map.of("file_name", "b.txt"));
        // 执行当前语句。
        Document thirdDocument = new Document("doc-3", Map.of("file_name", "c.txt"));
        // 执行当前语句。
        when(hybridRetrievalService.retrieve("question")).thenReturn(List.of(firstDocument, secondDocument, thirdDocument));
        // 执行当前语句。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 执行当前语句。
        assertThat(result.searchQuery()).isEqualTo("question");
        // 执行当前语句。
        assertThat(result.documents()).hasSize(2);
        // 执行当前语句。
        verify(queryRewriteService, never()).rewrite("question");
        // 执行当前语句。
        verify(queryDecompositionService, never()).decompose("question");
        // 执行当前语句。
        verify(rerankService, never()).rerank(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    // 结束当前代码块。
    }

    /**
     * 描述 `retrieveRewritesQueryAndDeduplicatesAcrossSubQueries` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void retrieveRewritesQueryAndDeduplicatesAcrossSubQueries() throws Exception {
        // 执行当前语句。
        ragProperties.setQueryRewriteEnabled(true);
        // 执行当前语句。
        ragProperties.setMultiQueryEnabled(true);
        // 执行当前语句。
        Document sharedDocument = Document.builder().id("shared-id").text("shared").metadata(Map.of("file_name", "shared.txt")).build();
        // 执行当前语句。
        Document uniqueDocument = Document.builder().id("unique-id").text("unique").metadata(Map.of("file_name", "unique.txt")).build();
        // 执行当前语句。
        when(queryRewriteService.rewrite("question")).thenReturn("rewritten question");
        // 执行当前语句。
        when(queryDecompositionService.decompose("rewritten question")).thenReturn(List.of("query-a", "query-b"));
        // 执行当前语句。
        when(hybridRetrievalService.retrieve("query-a")).thenReturn(List.of(sharedDocument));
        // 执行当前语句。
        when(hybridRetrievalService.retrieve("query-b")).thenReturn(List.of(sharedDocument, uniqueDocument));
        // 执行当前语句。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 执行当前语句。
        assertThat(result.searchQuery()).isEqualTo("rewritten question");
        // 执行当前语句。
        assertThat(result.documents()).hasSize(2);
        // 执行当前语句。
        assertThat(result.documents()).extracting(Document::getId).containsExactly("shared-id", "unique-id");
    // 结束当前代码块。
    }
// 结束当前代码块。
}
