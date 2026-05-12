// 声明当前源文件所属包。
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

// 为当前测试类启用指定扩展。
@ExtendWith(MockitoExtension.class)
// 定义 `RetrievalPipelineServiceTest` 类型。
class RetrievalPipelineServiceTest {

    // 声明ragproperties变量，供后续流程使用。
    private RagProperties ragProperties;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明查询改写service变量，供后续流程使用。
    private QueryRewriteService queryRewriteService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明查询拆分service变量，供后续流程使用。
    private QueryDecompositionService queryDecompositionService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明hybrid检索service变量，供后续流程使用。
    private HybridRetrievalService hybridRetrievalService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明重排service变量，供后续流程使用。
    private RerankService rerankService;

    // 将模拟依赖注入被测对象。
    @InjectMocks
    // 声明检索pipelineservice变量，供后续流程使用。
    private RetrievalPipelineService retrievalPipelineService;

    /**
     * 处理setup相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法在每个测试前执行。
    @BeforeEach
    void setUp() throws Exception {
        // 创建ragproperties对象。
        ragProperties = new RagProperties();
        // 更新查询改写enabled字段。
        ragProperties.setQueryRewriteEnabled(false);
        // 更新multi查询enabled字段。
        ragProperties.setMultiQueryEnabled(false);
        // 更新重排enabled字段。
        ragProperties.setRerankEnabled(false);
        // 更新finaltopk字段。
        ragProperties.setFinalTopK(2);
        // 更新field字段。
        org.springframework.test.util.ReflectionTestUtils.setField(retrievalPipelineService, "ragProperties", ragProperties);
    }

    /**
     * 处理retrieveusesshortestpathwhenoptionalstagesaredisabled相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void retrieveUsesShortestPathWhenOptionalStagesAreDisabled() throws Exception {
        // 创建first文档对象。
        Document firstDocument = new Document("doc-1", Map.of("file_name", "a.txt"));
        // 创建second文档对象。
        Document secondDocument = new Document("doc-2", Map.of("file_name", "b.txt"));
        // 创建third文档对象。
        Document thirdDocument = new Document("doc-3", Map.of("file_name", "c.txt"));
        // 为当前测试场景预设模拟对象行为。
        when(hybridRetrievalService.retrieve("question")).thenReturn(List.of(firstDocument, secondDocument, thirdDocument));
        // 执行检索流程并获取候选文档。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 断言当前结果符合测试预期。
        assertThat(result.searchQuery()).isEqualTo("question");
        // 断言当前结果符合测试预期。
        assertThat(result.documents()).hasSize(2);
        // 校验依赖调用是否符合预期。
        verify(queryRewriteService, never()).rewrite("question", null);
        // 校验依赖调用是否符合预期。
        verify(queryDecompositionService, never()).decompose("question", null);
        // 校验依赖调用是否符合预期。
        verify(rerankService, never()).rerank(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    /**
     * 处理retrieverewrites查询anddeduplicatesacross子queries相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void retrieveRewritesQueryAndDeduplicatesAcrossSubQueries() throws Exception {
        // 更新查询改写enabled字段。
        ragProperties.setQueryRewriteEnabled(true);
        // 更新multi查询enabled字段。
        ragProperties.setMultiQueryEnabled(true);
        // 开始构建shared文档对象。
        Document sharedDocument = Document.builder().id("shared-id").text("shared").metadata(Map.of("file_name", "shared.txt")).build();
        // 开始构建unique文档对象。
        Document uniqueDocument = Document.builder().id("unique-id").text("unique").metadata(Map.of("file_name", "unique.txt")).build();
        // 为当前测试场景预设模拟对象行为。
        when(queryRewriteService.rewrite("question", null)).thenReturn("rewritten question");
        // 为当前测试场景预设模拟对象行为。
        when(queryDecompositionService.decompose("rewritten question", null)).thenReturn(List.of("query-a", "query-b"));
        // 为当前测试场景预设模拟对象行为。
        when(hybridRetrievalService.retrieve("query-a")).thenReturn(List.of(sharedDocument));
        // 为当前测试场景预设模拟对象行为。
        when(hybridRetrievalService.retrieve("query-b")).thenReturn(List.of(sharedDocument, uniqueDocument));
        // 执行检索流程并获取候选文档。
        RetrievalResult result = retrievalPipelineService.retrieve("question");
        // 断言当前结果符合测试预期。
        assertThat(result.searchQuery()).isEqualTo("rewritten question");
        // 断言当前结果符合测试预期。
        assertThat(result.documents()).hasSize(2);
        // 断言当前结果符合测试预期。
        assertThat(result.documents()).extracting(Document::getId).containsExactly("shared-id", "unique-id");
    }
}
