// 声明测试类所在包。
package com.inote.service;

import com.inote.client.FallbackChatModel;
import com.inote.model.dto.ChatRequest;
import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.repository.ChatMessageRepository;
import com.inote.service.retrieval.RetrievalPipelineService;
import com.inote.service.retrieval.RetrievalResult;
import com.inote.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 标记当前类使用 Mockito 扩展。
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    // 声明主聊天模型模拟对象。
    @Mock
    private ChatModel chatModel;

    // 声明容错聊天模型模拟对象。
    @Mock
    private FallbackChatModel fallbackChatModel;

    // 声明检索流水线模拟对象。
    @Mock
    private RetrievalPipelineService retrievalPipelineService;

    // 声明会话服务模拟对象。
    @Mock
    private ChatSessionService chatSessionService;

    // 声明消息仓储模拟对象。
    @Mock
    private ChatMessageRepository chatMessageRepository;

    // 声明被测服务实例。
    @InjectMocks
    private ChatService chatService;

    /**
     * 验证没有检索结果且没有会话时不会持久化消息。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void queryReturnsFallbackAnswerWithoutPersistingWhenSessionIsMissing() throws Exception {
        // 构造无会话请求对象。
        ChatRequest request = ChatRequest.builder().question("What is inote?").build();
        // 模拟检索结果为空。
        when(retrievalPipelineService.retrieve("What is inote?")).thenReturn(new RetrievalResult("What is inote?", "What is inote?", List.of()));
        // 调用查询方法。
        var response = chatService.query(request);
        // 断言返回兜底回答。
        assertThat(response.getAnswer()).isEqualTo("The current documents do not provide enough information to answer this question.");
        // 断言未创建会话标识。
        assertThat(response.getSessionId()).isNull();
        // 断言没有调用模型。
        verify(fallbackChatModel, never()).callWithFallback(any(ChatModel.class), any(Prompt.class));
        // 断言没有落库消息。
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    /**
     * 验证无文档结果但有会话时会持久化用户和助手消息并更新标题。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void queryPersistsConversationAndRenamesDefaultSessionWhenFirstTurnHasNoDocs() throws Exception {
        // 构造测试用户。
        var user = TestDataFactory.user("user-1", "tester", "token-1");
        // 构造默认标题会话。
        ChatSession session = TestDataFactory.session("session-1", user, "New Session");
        // 构造带会话请求对象。
        ChatRequest request = ChatRequest.builder().sessionId("session-1").question("How should we start this migration?").build();
        // 模拟会话解析结果。
        when(chatSessionService.getSessionEntity("session-1")).thenReturn(session);
        // 模拟历史消息为空。
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());
        // 模拟检索结果为空。
        when(retrievalPipelineService.retrieve("How should we start this migration?")).thenReturn(new RetrievalResult("How should we start this migration?", "How should we start this migration?", List.of()));
        // 调用查询方法。
        var response = chatService.query(request);
        // 捕获被保存的消息实体。
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        // 断言消息被保存两次。
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        // 断言返回会话标识。
        assertThat(response.getSessionId()).isEqualTo("session-1");
        // 断言会话标题已从默认值改为问题摘要。
        assertThat(session.getTitle()).isEqualTo("How should we start ...");
        // 断言首条消息角色为用户。
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
        // 断言第二条消息角色为助手。
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        // 断言会话刷新方法被调用。
        verify(chatSessionService).touchSession(session);
    }

    /**
     * 验证检索到文档时会调用模型并提取去重后的来源。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void queryUsesModelWhenRelevantDocumentsExist() throws Exception {
        // 构造测试文档一。
        Document firstDocument = new Document("first chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 构造测试文档二并复用相同来源以覆盖去重逻辑。
        Document secondDocument = new Document("second chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 构造模拟模型响应。
        ChatResponse modelResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("generated answer"))));
        // 构造查询请求对象。
        ChatRequest request = ChatRequest.builder().question("Summarize the uploaded files").build();
        // 模拟检索流水线返回文档。
        when(retrievalPipelineService.retrieve("Summarize the uploaded files")).thenReturn(new RetrievalResult("Summarize the uploaded files", "Summarize the uploaded files", List.of(firstDocument, secondDocument)));
        // 模拟容错模型返回正常回答。
        when(fallbackChatModel.callWithFallback(eq(chatModel), any(Prompt.class))).thenReturn(modelResponse);
        // 调用查询方法。
        var response = chatService.query(request);
        // 断言调用模型后返回生成答案。
        assertThat(response.getAnswer()).isEqualTo("generated answer");
        // 断言来源列表按 URL 去重。
        assertThat(response.getSources()).hasSize(1);
        // 断言来源文件名映射正确。
        assertThat(response.getSources().get(0).getFileName()).isEqualTo("doc-1.txt");
    }

    /**
     * 验证模型异常时会返回服务不可用文案。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void queryReturnsServiceUnavailableMessageWhenModelThrowsException() throws Exception {
        // 构造单个相关文档。
        Document document = new Document("knowledge chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 构造查询请求对象。
        ChatRequest request = ChatRequest.builder().question("Answer from docs").build();
        // 模拟检索流水线返回相关文档。
        when(retrievalPipelineService.retrieve("Answer from docs")).thenReturn(new RetrievalResult("Answer from docs", "Answer from docs", List.of(document)));
        // 模拟模型调用抛出异常。
        when(fallbackChatModel.callWithFallback(eq(chatModel), any(Prompt.class))).thenThrow(new RuntimeException("boom"));
        // 调用查询方法。
        var response = chatService.query(request);
        // 断言返回服务不可用提示。
        assertThat(response.getAnswer()).isEqualTo("The service is temporarily unavailable. Please try again later.");
    }
}
