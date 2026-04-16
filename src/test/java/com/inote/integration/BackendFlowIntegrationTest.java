// 声明当前源文件的包。
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

// 应用当前注解。
@SpringBootTest
// 应用当前注解。
@AutoConfigureMockMvc
// 应用当前注解。
@ActiveProfiles("test")
// 声明当前类型。
class BackendFlowIntegrationTest {

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private MockMvc mockMvc;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private UserRepository userRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ChatSessionRepository chatSessionRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ChatMessageRepository chatMessageRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private DocumentRepository documentRepository;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private com.inote.controller.DocumentController documentController;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private com.inote.service.DocumentService documentService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private OpenAiChatModel openAiChatModel;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private ChatModel chatModel;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private FallbackChatModel fallbackChatModel;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private EmbeddingModel embeddingModel;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private VectorStore vectorStore;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private MilvusServiceClient milvusServiceClient;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private RetrievalPipelineService retrievalPipelineService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private RerankService rerankService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private DocumentProcessingService documentProcessingService;

    // 应用当前注解。
    @TempDir
    // 声明当前字段。
    Path tempDir;

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
        chatMessageRepository.deleteAll();
        // 执行当前语句。
        chatSessionRepository.deleteAll();
        // 执行当前语句。
        documentRepository.deleteAll();
        // 执行当前语句。
        userRepository.deleteAll();
        // 执行当前语句。
        Files.createDirectories(tempDir.resolve("uploads"));
        // 执行当前语句。
        ReflectionTestUtils.setField(documentController, "uploadPath", tempDir.resolve("uploads").toString());
        // 执行当前语句。
        ReflectionTestUtils.setField(documentService, "uploadPath", tempDir.resolve("uploads").toString());
    // 结束当前代码块。
    }

    /**
     * 描述 `chatQueryPersistsMessagesForAuthenticatedUser` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void chatQueryPersistsMessagesForAuthenticatedUser() throws Exception {
        // 执行当前语句。
        User user = userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 执行当前语句。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", user, "New Session"));
        // 执行当前语句。
        when(retrievalPipelineService.retrieve("What is the rollout plan?")).thenReturn(new RetrievalResult("What is the rollout plan?", "What is the rollout plan?", List.of()));
        mockMvc.perform(post("/api/v1/chat/query").header("X-Auth-Token", "token-1").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "sessionId": "session-1",
                  "question": "What is the rollout plan?"
                }
                """))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("The current documents do not provide enough information to answer this question."));
        // 执行当前语句。
        assertThat(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())).hasSize(2);
        // 执行当前语句。
        assertThat(chatSessionRepository.findById(session.getId())).isPresent();
        // 执行当前语句。
        assertThat(chatSessionRepository.findById(session.getId()).orElseThrow().getTitle()).isEqualTo("What is the rollout ...");
    // 结束当前代码块。
    }

    /**
     * 描述 `documentUploadCreatesOwnedDocumentRecord` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void documentUploadCreatesOwnedDocumentRecord() throws Exception {
        // 执行当前语句。
        userRepository.save(TestDataFactory.user("user-1", "tester", "token-1"));
        // 执行当前语句。
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello upload".getBytes());
        // 执行当前语句。
        when(documentProcessingService.saveFile(any(), anyString())).thenReturn(tempDir.resolve("uploads").resolve("saved-notes.txt"));
        // 执行当前语句。
        doNothing().when(documentProcessingService).processDocumentAsync(any(Document.class), any(), anyString());
        // 处理当前代码结构。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file).header("X-Auth-Token", "token-1"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 执行当前语句。
                .andExpect(jsonPath("$.status").value("PARSING"));
        // 执行当前语句。
        assertThat(documentRepository.findAll()).hasSize(1);
        // 执行当前语句。
        assertThat(documentRepository.findAll().get(0).getOwner().getUsername()).isEqualTo("tester");
    // 结束当前代码块。
    }

    /**
     * 描述 `ownershipBoundariesReturnNotFoundForAnotherUser` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void ownershipBoundariesReturnNotFoundForAnotherUser() throws Exception {
        // 执行当前语句。
        User owner = userRepository.save(TestDataFactory.user("user-1", "owner", "owner-token"));
        // 执行当前语句。
        userRepository.save(TestDataFactory.user("user-2", "other", "other-token"));
        // 执行当前语句。
        ChatSession session = chatSessionRepository.save(TestDataFactory.session("session-1", owner, "Owner Session"));
        // 执行当前语句。
        Document document = documentRepository.save(TestDataFactory.document("doc-1", owner, "COMPLETED"));
        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions/" + session.getId()).header("X-Auth-Token", "other-token"))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Session not found: " + session.getId()));
        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/" + document.getId()).header("X-Auth-Token", "other-token"))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Document not found: " + document.getId()));
    // 结束当前代码块。
    }
// 结束当前代码块。
}
