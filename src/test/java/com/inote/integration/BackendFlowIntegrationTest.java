// 声明测试类所在包。
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
import org.springframework.ai.chat.model.ChatModel;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 标记当前类为 Spring Boot 集成测试。
@SpringBootTest
// 启用 MockMvc 支持。
@AutoConfigureMockMvc
// 指定当前测试启用 test 配置文件。
@ActiveProfiles("test")
class BackendFlowIntegrationTest {

    // 注入 MockMvc。
    @Autowired
    private MockMvc mockMvc;

    // 注入用户仓储。
    @Autowired
    private UserRepository userRepository;

    // 注入会话仓储。
    @Autowired
    private ChatSessionRepository chatSessionRepository;

    // 注入消息仓储。
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 注入文档仓储。
    @Autowired
    private DocumentRepository documentRepository;

    // 注入文档控制器。
    @Autowired
    private com.inote.controller.DocumentController documentController;

    // 注入文档服务。
    @Autowired
    private com.inote.service.DocumentService documentService;

    // 模拟 OpenAI 聊天模型。
    @MockBean
    private OpenAiChatModel openAiChatModel;

    // 模拟聊天模型接口。
    @MockBean
    private ChatModel chatModel;

    // 模拟容错聊天模型。
    @MockBean
    private FallbackChatModel fallbackChatModel;

    // 模拟嵌入模型。
    @MockBean
    private EmbeddingModel embeddingModel;

    // 模拟向量存储。
    @MockBean
    private VectorStore vectorStore;

    // 模拟 Milvus 客户端。
    @MockBean
    private MilvusServiceClient milvusServiceClient;

    // 模拟检索流水线。
    @MockBean
    private RetrievalPipelineService retrievalPipelineService;

    // 模拟重排服务。
    @MockBean
    private RerankService rerankService;

    // 模拟文档异步处理服务。
    @MockBean
    private DocumentProcessingService documentProcessingService;

    // 注入测试临时目录。
    @TempDir
    Path tempDir;

    /**
     * 初始化集成测试环境。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当目录创建或反射写入失败时抛出异常。
     */
    @BeforeEach
    void setUp() throws Exception {
        // 清空消息表。
        chatMessageRepository.deleteAll();
        // 清空会话表。
        chatSessionRepository.deleteAll();
        // 清空文档表。
        documentRepository.deleteAll();
        // 清空用户表。
        userRepository.deleteAll();
        // 创建测试上传目录。
        Files.createDirectories(tempDir.resolve("uploads"));
        // 覆写控制器中的上传目录。
        ReflectionTestUtils.setField(documentController, "uploadPath", tempDir.resolve("uploads").toString());
        // 覆写服务中的上传目录。
        ReflectionTestUtils.setField(documentService, "uploadPath", tempDir.resolve("uploads").toString());
    }

    /**
     * 验证认证后发送查询会落库双向消息。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当 MockMvc 调用失败时抛出异常。
     */
    @Test
    void chatQueryPersistsMessagesForAuthenticatedUser() throws Exception {
        // 保存测试用户。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 保存默认标题会话。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "New Session"));
        // 模拟检索流水线返回空结果。
        when(retrievalPipelineService.retrieve("What is the rollout plan?")).thenReturn(new RetrievalResult("What is the rollout plan?", "What is the rollout plan?", List.of()));
        // 执行聊天查询请求。
        mockMvc.perform(post("/api/v1/chat/query").header("X-Auth-Token", "token-1").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "sessionId": "session-1",
                  "question": "What is the rollout plan?"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.answer").value("The current documents do not provide enough information to answer this question."));
        // 断言消息已保存两条。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).hasSize(2);
        // 断言会话记录仍然存在。
        assertThat(chatSessionRepository.findById(session.getId())).isPresent();
        // 断言默认标题已被更新为问题摘要。
        assertThat(chatSessionRepository.findById(session.getId()).orElseThrow().getTitle()).isEqualTo("What is the rollout ...");
    }

    /**
     * 验证认证后上传文件会创建当前用户文档记录。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当 MockMvc 调用失败时抛出异常。
     */
    @Test
    void documentUploadCreatesOwnedDocumentRecord() throws Exception {
        // 保存测试用户。
        userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 构造上传文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello upload".getBytes());
        // 模拟文件保存路径。
        when(documentProcessingService.saveFile(any(), anyString())).thenReturn(tempDir.resolve("uploads").resolve("saved-notes.txt"));
        // 模拟异步处理为空操作。
        doNothing().when(documentProcessingService).processDocumentAsync(any(Document.class), any(), anyString());
        // 执行上传请求。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file).header("X-Auth-Token", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARSING"));
        // 断言数据库中已生成文档记录。
        assertThat(documentRepository.findAll()).hasSize(1);
        // 断言文档归属当前用户。
        assertThat(documentRepository.findAll().get(0).getOwner().getUsername()).isEqualTo("tester");
    }

    /**
     * 验证跨用户访问他人资源会返回未找到。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当 MockMvc 调用失败时抛出异常。
     */
    @Test
    void ownershipBoundariesReturnNotFoundForAnotherUser() throws Exception {
        // 保存资源所有者。
        User owner = userRepository.save(TestDataFactory.user("user-1", "owner", "owner-token"));
        // 保存其他用户。
        userRepository.save(TestDataFactory.user("user-2", "other", "other-token"));
        // 保存所有者会话。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", owner, "Owner Session"));
        // 保存所有者文档。
        Document document = documentRepository.save(TestDataFactory.document("doc-1", owner, "COMPLETED"));
        // 执行跨用户会话查询。
        mockMvc.perform(get("/api/v1/chat/sessions/" + session.getId()).header("X-Auth-Token", "other-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: " + session.getId()));
        // 执行跨用户文档状态查询。
        mockMvc.perform(get("/api/v1/documents/" + document.getId()).header("X-Auth-Token", "other-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found: " + document.getId()));
    }
}
