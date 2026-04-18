// 声明当前源文件所属包。
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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 为当前测试类启用指定扩展。
@ExtendWith(MockitoExtension.class)
// 定义 `ChatServiceTest` 类型。
class ChatServiceTest {

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明问答模型变量，供后续流程使用。
    private ChatModel chatModel;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明兜底问答模型变量，供后续流程使用。
    private FallbackChatModel fallbackChatModel;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明检索pipelineservice变量，供后续流程使用。
    private RetrievalPipelineService retrievalPipelineService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明问答会话service变量，供后续流程使用。
    private ChatSessionService chatSessionService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明问答消息repository变量，供后续流程使用。
    private ChatMessageRepository chatMessageRepository;

    // 将模拟依赖注入被测对象。
    @InjectMocks
    // 声明问答service变量，供后续流程使用。
    private ChatService chatService;

    /**
     * 处理查询returns兜底回答withoutpersistingwhen会话ismissing相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryReturnsFallbackAnswerWithoutPersistingWhenSessionIsMissing() throws Exception {
        // 开始构建请求对象。
        ChatRequest request = ChatRequest.builder().question("What is inote?").build();
        // 为当前测试场景预设模拟对象行为。
        when(retrievalPipelineService.retrieve("What is inote?")).thenReturn(new RetrievalResult("What is inote?", "What is inote?", List.of()));
        // 计算并保存响应结果。
        var response = chatService.query(request);
        // 断言当前结果符合测试预期。
        assertThat(response.getAnswer()).isEqualTo("The current documents do not provide enough information to answer this question.");
        // 断言当前结果符合测试预期。
        assertThat(response.getSessionId()).isNull();
        // 定义当前类型。
        verify(fallbackChatModel, never()).callWithFallback(any(ChatModel.class), any(Prompt.class));
        // 定义当前类型。
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    /**
     * 处理查询persistsconversationandrenamesdefault会话whenfirstturnhasnodocs相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryPersistsConversationAndRenamesDefaultSessionWhenFirstTurnHasNoDocs() throws Exception {
        // 计算并保存用户结果。
        var user = TestDataFactory.user("user-1", "tester", "token-1");
        // 计算并保存会话结果。
        ChatSession session = TestDataFactory.session("session-1", user, "New Session");
        // 开始构建请求对象。
        ChatRequest request = ChatRequest.builder().sessionId("session-1").question("How should we start this migration?").build();
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.getSessionEntity("session-1")).thenReturn(session);
        // 为当前测试场景预设模拟对象行为。
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());
        // 为当前测试场景预设模拟对象行为。
        when(retrievalPipelineService.retrieve("How should we start this migration?")).thenReturn(new RetrievalResult("How should we start this migration?", "How should we start this migration?", List.of()));
        // 计算并保存响应结果。
        var response = chatService.query(request);
        // 定义当前类型。
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        // 校验依赖调用是否符合预期。
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        // 断言当前结果符合测试预期。
        assertThat(response.getSessionId()).isEqualTo("session-1");
        // 断言当前结果符合测试预期。
        assertThat(session.getTitle()).isEqualTo("How should we start ...");
        // 断言当前结果符合测试预期。
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
        // 断言当前结果符合测试预期。
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        // 校验依赖调用是否符合预期。
        verify(chatSessionService).touchSession(session);
    }

    /**
     * 处理查询uses模型whenrelevant文档exist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryUsesModelWhenRelevantDocumentsExist() throws Exception {
        // 创建first文档对象。
        Document firstDocument = new Document("first chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 创建second文档对象。
        Document secondDocument = new Document("second chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 创建模型响应对象。
        ChatResponse modelResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("generated answer"))));
        // 开始构建请求对象。
        ChatRequest request = ChatRequest.builder().question("Summarize the uploaded files").build();
        // 为当前测试场景预设模拟对象行为。
        when(retrievalPipelineService.retrieve("Summarize the uploaded files")).thenReturn(new RetrievalResult("Summarize the uploaded files", "Summarize the uploaded files", List.of(firstDocument, secondDocument)));
        // 定义当前类型。
        doReturn(modelResponse)
                .when(fallbackChatModel)
                .callWithFallback(isA(ChatModel.class), isA(Prompt.class));
        // 计算并保存响应结果。
        var response = chatService.query(request);
        // 断言当前结果符合测试预期。
        assertThat(response.getAnswer()).isEqualTo("generated answer");
        // 断言当前结果符合测试预期。
        assertThat(response.getSources()).hasSize(1);
        // 断言当前结果符合测试预期。
        assertThat(response.getSources().get(0).getFileName()).isEqualTo("doc-1.txt");
    }

    /**
     * 处理查询returnsserviceunavailable消息when模型throwsexception相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryReturnsServiceUnavailableMessageWhenModelThrowsException() throws Exception {
        // 创建文档对象。
        Document document = new Document("knowledge chunk", Map.of("file_name", "doc-1.txt", "file_url", "/files/1"));
        // 开始构建请求对象。
        ChatRequest request = ChatRequest.builder().question("Answer from docs").build();
        // 为当前测试场景预设模拟对象行为。
        when(retrievalPipelineService.retrieve("Answer from docs")).thenReturn(new RetrievalResult("Answer from docs", "Answer from docs", List.of(document)));
        // 定义当前类型。
        doThrow(new RuntimeException("boom"))
                .when(fallbackChatModel)
                .callWithFallback(isA(ChatModel.class), isA(Prompt.class));
        // 计算并保存响应结果。
        var response = chatService.query(request);
        // 断言当前结果符合测试预期。
        assertThat(response.getAnswer()).isEqualTo("The service is temporarily unavailable. Please try again later.");
    }
}
