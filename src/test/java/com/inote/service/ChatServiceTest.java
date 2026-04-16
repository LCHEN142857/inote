// 声明当前源文件的包。
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

// 应用当前注解。
@ExtendWith(MockitoExtension.class)
// 声明当前类型。
class ChatServiceTest {

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private ChatModel chatModel;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private FallbackChatModel fallbackChatModel;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private RetrievalPipelineService retrievalPipelineService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private ChatSessionService chatSessionService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private ChatMessageRepository chatMessageRepository;

    // 应用当前注解。
    @InjectMocks
    // 声明当前字段。
    private ChatService chatService;

    /**
     * 描述 `queryReturnsFallbackAnswerWithoutPersistingWhenSessionIsMissing` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryReturnsFallbackAnswerWithoutPersistingWhenSessionIsMissing() throws Exception {
        // 执行当前语句。
        ChatRequest request = ChatRequest.builder().question("What is inote?").build();
        // 执行当前语句。
        when(retrievalPipelineService.retrieve("What is inote?")).thenReturn(new RetrievalResult("What is inote?", "What is inote?", List.of()));
        // 执行当前语句。
        var response = chatService.query(request);
        // 执行当前语句。
        assertThat(response.getAnswer()).isEqualTo("The current documents do not provide enough information to answer this question.");
        // 执行当前语句。
        assertThat(response.getSessionId()).isNull();
        // 执行当前语句。
        verify(fallbackChatModel, never()).callWithFallback(any(ChatModel.class), any(Prompt.class));
        // 执行当前语句。
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    // 结束当前代码块。
    }

    /**
     * 描述 `queryPersistsConversationAndRenamesDefaultSessionWhenFirstTurnHasNoDocs` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryPersistsConversationAndRenamesDefaultSessionWhenFirstTurnHasNoDocs() throws Exception {
        // 执行当前语句。
        var user = TestDataFactory.user("user-1", "tester", "token-1");
        // 执行当前语句。
        ChatSession session = TestDataFactory.session("session-1", user, "New Session");
        // 执行当前语句。
        ChatRequest request = ChatRequest.builder().sessionId("session-1").question("How should we start this migration?").build();
        // 执行当前语句。
        when(chatSessionService.getSessionEntity("session-1")).thenReturn(session);
        // 执行当前语句。
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());
        // 执行当前语句。
        when(retrievalPipelineService.retrieve("How should we start this migration?")).thenReturn(new RetrievalResult("How should we start this migration?", "How should we start this migration?", List.of()));
        // 执行当前语句。
        var response = chatService.query(request);
        // 执行当前语句。
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        // 执行当前语句。
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        // 执行当前语句。
        assertThat(response.getSessionId()).isEqualTo("session-1");
        // 执行当前语句。
        assertThat(session.getTitle()).isEqualTo("How should we start ...");
        // 执行当前语句。
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
        // 执行当前语句。
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        // 执行当前语句。
        verify(chatSessionService).touchSession(session);
    // 结束当前代码块。
    }

    /**
     * 描述 `queryUsesModelWhenRelevantDocumentsExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryUsesModelWhenRelevantDocumentsExist() throws Exception {
        // 执行当前语句。
        Document firstDocument = new Document("first chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 执行当前语句。
        Document secondDocument = new Document("second chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 执行当前语句。
        ChatResponse modelResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("generated answer"))));
        // 执行当前语句。
        ChatRequest request = ChatRequest.builder().question("Summarize the uploaded files").build();
        // 执行当前语句。
        when(retrievalPipelineService.retrieve("Summarize the uploaded files")).thenReturn(new RetrievalResult("Summarize the uploaded files", "Summarize the uploaded files", List.of(firstDocument, secondDocument)));
        // 执行当前语句。
        when(fallbackChatModel.callWithFallback(eq(chatModel), any(Prompt.class))).thenReturn(modelResponse);
        // 执行当前语句。
        var response = chatService.query(request);
        // 执行当前语句。
        assertThat(response.getAnswer()).isEqualTo("generated answer");
        // 执行当前语句。
        assertThat(response.getSources()).hasSize(1);
        // 执行当前语句。
        assertThat(response.getSources().get(0).getFileName()).isEqualTo("doc-1.txt");
    // 结束当前代码块。
    }

    /**
     * 描述 `queryReturnsServiceUnavailableMessageWhenModelThrowsException` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryReturnsServiceUnavailableMessageWhenModelThrowsException() throws Exception {
        // 执行当前语句。
        Document document = new Document("knowledge chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 执行当前语句。
        ChatRequest request = ChatRequest.builder().question("Answer from docs").build();
        // 执行当前语句。
        when(retrievalPipelineService.retrieve("Answer from docs")).thenReturn(new RetrievalResult("Answer from docs", "Answer from docs", List.of(document)));
        // 执行当前语句。
        when(fallbackChatModel.callWithFallback(eq(chatModel), any(Prompt.class))).thenThrow(new RuntimeException("boom"));
        // 执行当前语句。
        var response = chatService.query(request);
        // 执行当前语句。
        assertThat(response.getAnswer()).isEqualTo("The service is temporarily unavailable. Please try again later.");
    // 结束当前代码块。
    }
// 结束当前代码块。
}
