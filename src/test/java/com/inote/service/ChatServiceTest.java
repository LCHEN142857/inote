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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private FallbackChatModel fallbackChatModel;

    @Mock
    private RetrievalPipelineService retrievalPipelineService;

    @Mock
    private ChatSessionService chatSessionService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private ChatModelSelectionService chatModelSelectionService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private ChatService chatService;

    @Test
    void queryReturnsFallbackAnswerWithoutPersistingWhenSessionIsMissing() {
        ChatRequest request = ChatRequest.builder().question("What is inote?").build();
        givenResolvedModel(null, "qwen3.5-plus-2026-02-15");
        when(userSettingsService.answerFromReferencesOnly()).thenReturn(true);
        when(retrievalPipelineService.retrieve("What is inote?", "qwen3.5-plus-2026-02-15"))
                .thenReturn(new RetrievalResult("What is inote?", "What is inote?", List.of()));

        var response = chatService.query(request);

        assertThat(response.getAnswer()).isEqualTo("当前文档信息不足，无法回答这个问题。");
        assertThat(response.getSessionId()).isNull();
        verify(fallbackChatModel, never()).callWithFallback(any(ChatModel.class), any(Prompt.class));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void queryPersistsConversationAndRenamesDefaultSessionWhenFirstTurnHasNoDocs() {
        var user = TestDataFactory.user("user-1", "tester", "token-1");
        ChatSession session = TestDataFactory.session("session-1", user, "New Session");
        ChatRequest request = ChatRequest.builder()
                .sessionId("session-1")
                .question("How should we start this migration?")
                .build();
        givenResolvedModel(null, "qwen3.5-plus-2026-02-15");
        when(userSettingsService.answerFromReferencesOnly()).thenReturn(true);
        when(chatSessionService.getSessionEntity("session-1")).thenReturn(session);
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());
        when(retrievalPipelineService.retrieve("How should we start this migration?", "qwen3.5-plus-2026-02-15"))
                .thenReturn(new RetrievalResult("How should we start this migration?", "How should we start this migration?", List.of()));

        var response = chatService.query(request);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(session.getTitle()).isEqualTo("How should we start ...");
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        verify(chatSessionService).touchSession(session);
    }

    @Test
    void queryUsesRequestedModelInPromptWhenRelevantDocumentsExist() {
        Document firstDocument = new Document("first chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        Document secondDocument = new Document("second chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        ChatResponse modelResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("generated answer"))));
        ChatRequest request = ChatRequest.builder()
                .question("Summarize the uploaded files")
                .model("glm-5")
                .build();
        givenSelectedModel("glm-5", "glm-5");
        when(retrievalPipelineService.retrieve("Summarize the uploaded files", "glm-5"))
                .thenReturn(new RetrievalResult("Summarize the uploaded files", "Summarize the uploaded files", List.of(firstDocument, secondDocument)));
        doReturn(modelResponse)
                .when(fallbackChatModel)
                .callWithFallback(isA(ChatModel.class), isA(Prompt.class));

        var response = chatService.query(request);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(fallbackChatModel).callWithFallback(isA(ChatModel.class), promptCaptor.capture());
        assertThat(response.getAnswer()).isEqualTo("generated answer");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getFileName()).isEqualTo("doc-1.txt");
        assertThat(promptCaptor.getValue().getOptions().getModel()).isEqualTo("glm-5");
    }

    @Test
    void queryReturnsServiceUnavailableMessageWhenModelThrowsException() {
        Document document = new Document("knowledge chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        ChatRequest request = ChatRequest.builder().question("Answer from docs").build();
        givenSelectedModel(null, "qwen3.5-plus-2026-02-15");
        when(retrievalPipelineService.retrieve("Answer from docs", "qwen3.5-plus-2026-02-15"))
                .thenReturn(new RetrievalResult("Answer from docs", "Answer from docs", List.of(document)));
        doThrow(new RuntimeException("boom"))
                .when(fallbackChatModel)
                .callWithFallback(isA(ChatModel.class), isA(Prompt.class));

        var response = chatService.query(request);

        assertThat(response.getAnswer()).isEqualTo("The service is temporarily unavailable. Please try again later.");
    }

    private void givenSelectedModel(String requestedModel, String resolvedModel) {
        givenResolvedModel(requestedModel, resolvedModel);
        when(chatModelSelectionService.buildOptions(resolvedModel))
                .thenReturn(OpenAiChatOptions.builder().model(resolvedModel).build());
    }

    private void givenResolvedModel(String requestedModel, String resolvedModel) {
        when(chatModelSelectionService.resolveModel(requestedModel)).thenReturn(resolvedModel);
    }
}
