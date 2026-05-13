// 声明当前源文件所属包。
package com.inote.integration;

import com.inote.client.FallbackChatModel;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;
import com.inote.repository.ChatMessageRepository;
import com.inote.repository.ChatSessionRepository;
import com.inote.repository.DocumentRepository;
import com.inote.repository.UserRepository;
import com.inote.service.DocumentProcessingService;
import com.inote.service.retrieval.RerankService;
import com.inote.service.retrieval.RetrievalPipelineService;
import com.inote.service.retrieval.RetrievalResult;
import com.inote.support.TestDataFactory;
import io.milvus.client.MilvusServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 加载完整 Spring Boot 上下文执行测试。
@SpringBootTest
// 在测试环境中注入 MockMvc。
@AutoConfigureMockMvc
// 声明测试运行时使用的配置环境。
@ActiveProfiles("test")
// 定义 `BackendFlowIntegrationTest` 类型。
class BackendFlowIntegrationTest {

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明mockmvc变量，供后续流程使用。
    private MockMvc mockMvc;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明问答会话repository变量，供后续流程使用。
    private ChatSessionRepository chatSessionRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明问答消息repository变量，供后续流程使用。
    private ChatMessageRepository chatMessageRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明文档repository变量，供后续流程使用。
    private DocumentRepository documentRepository;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 围绕cominotecontroller补充当前业务语句。
    private com.inote.controller.DocumentController documentController;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 围绕cominoteservice补充当前业务语句。
    private com.inote.service.DocumentService documentService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明openai问答模型变量，供后续流程使用。
    private OpenAiChatModel openAiChatModel;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明问答模型变量，供后续流程使用。
    private ChatModel chatModel;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明兜底问答模型变量，供后续流程使用。
    private FallbackChatModel fallbackChatModel;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明向量模型变量，供后续流程使用。
    private EmbeddingModel embeddingModel;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明向量store变量，供后续流程使用。
    private VectorStore vectorStore;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明milvusserviceclient变量，供后续流程使用。
    private MilvusServiceClient milvusServiceClient;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明检索pipelineservice变量，供后续流程使用。
    private RetrievalPipelineService retrievalPipelineService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明重排service变量，供后续流程使用。
    private RerankService rerankService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明文档processingservice变量，供后续流程使用。
    private DocumentProcessingService documentProcessingService;

    // 为当前测试注入临时目录。
    @TempDir
    // 声明tempdir变量，供后续流程使用。
    Path tempDir;

    /**
     * 处理setup相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法在每个测试前执行。
    @BeforeEach
    void setUp() throws Exception {
        // 删除当前持久化数据。
        chatMessageRepository.deleteAll();
        // 删除当前持久化数据。
        chatSessionRepository.deleteAll();
        // 删除当前持久化数据。
        documentRepository.deleteAll();
        // 删除当前持久化数据。
        userRepository.deleteAll();
        // 调用 `createDirectories` 完成当前步骤。
        Files.createDirectories(tempDir.resolve("uploads"));
        // 更新field字段。
        ReflectionTestUtils.setField(documentController, "uploadPath", tempDir.resolve("uploads").toString());
        // 更新field字段。
        ReflectionTestUtils.setField(documentService, "uploadPath", tempDir.resolve("uploads").toString());
    }

    /**
     * 处理问答查询persists消息forauthenticated用户相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void chatQueryPersistsMessagesForAuthenticatedUser() throws Exception {
        // 保存用户对象。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        user.setAnswerFromReferencesOnly(false);
        user = userRepository.save(user);
        // 保存会话对象。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "New Session"));
        // 为当前测试场景预设模拟对象行为。
        when(retrievalPipelineService.retrieve("What is the rollout plan?", "kimi-k2.6"))
                .thenReturn(new RetrievalResult("What is the rollout plan?", "What is the rollout plan?", List.of()));
        when(fallbackChatModel.callWithFallback(isA(ChatModel.class), isA(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("未匹配到可用参考文档，以下基于通用知识回答：先明确目标和时间线。")))));
        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/query").header("X-Auth-Token", "token-1").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "sessionId": "session-1",
                  "question": "What is the rollout plan?"
                }
                """))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("未匹配到可用参考文档，以下基于通用知识回答：先明确目标和时间线。"));
        // 断言当前结果符合测试预期。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).hasSize(2);
        // 断言当前结果符合测试预期。
        assertThat(chatSessionRepository.findById(session.getId())).isPresent();
        // 断言当前结果符合测试预期。
        assertThat(chatSessionRepository.findById(session.getId()).orElseThrow().getTitle()).isEqualTo("What is the rollout ...");
    }

    /**
     * 处理文档上传createsowned文档record相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void documentUploadCreatesOwnedDocumentRecord() throws Exception {
        // 保存当前对象到持久化层。
        userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 创建文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello upload".getBytes());
        // 为当前测试场景预设模拟对象行为。
        when(documentProcessingService.saveFile(any(), anyString())).thenReturn(tempDir.resolve("uploads").resolve("saved-notes.txt"));
        // 定义当前类型。
        doNothing().when(documentProcessingService).processDocumentAsync(anyString(), anyString());
        // 发起当前接口的集成测试请求。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file).header("X-Auth-Token", "token-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.status").value("PARSING"));
        // 断言当前结果符合测试预期。
        assertThat(documentRepository.findAll()).hasSize(1);
        // 断言当前结果符合测试预期。
        assertThat(documentRepository.findAll().get(0).getOwner().getUsername()).isEqualTo("tester");
    }

    /**
     * 处理ownershipboundariesreturnnotfoundforanother用户相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void ownershipBoundariesReturnNotFoundForAnotherUser() throws Exception {
        // 保存所属用户对象。
        User owner = userRepository.save(TestDataFactory.user("user-1", "owner", "owner-token"));
        // 保存当前对象到持久化层。
        userRepository.save(TestDataFactory.user("user-2", "other", "other-token"));
        // 保存会话对象。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", owner, "Owner Session"));
        // 保存文档对象。
        Document document = documentRepository.save(TestDataFactory.document("doc-1", owner, "COMPLETED"));
        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions/" + session.getId()).header("X-Auth-Token", "other-token"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Session not found: " + session.getId()));
        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/" + document.getId()).header("X-Auth-Token", "other-token"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Document not found: " + document.getId()));
    }
}
