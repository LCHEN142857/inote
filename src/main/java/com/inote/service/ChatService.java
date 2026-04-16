// 声明当前源文件的包。
package com.inote.service;

import com.inote.client.FallbackChatModel;
import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.InoteResponse;
import com.inote.model.dto.SourceReference;
import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.repository.ChatMessageRepository;
import com.inote.service.retrieval.RetrievalPipelineService;
import com.inote.service.retrieval.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are an enterprise knowledge-base assistant.

            Rules:
            1. Answer only from the provided reference documents.
            2. If the documents do not contain the answer, say that the current documents do not provide enough information.
            3. When chat history exists, use it to understand follow-up questions.
            4. Keep the answer concise and accurate, and cite sources when possible.

            Reference documents:
            {context}
            """;

    // 声明当前字段。
    private static final int MAX_HISTORY_MESSAGES = 20;

    // 声明当前字段。
    private final ChatModel chatModel;
    // 声明当前字段。
    private final FallbackChatModel fallbackChatModel;
    // 声明当前字段。
    private final RetrievalPipelineService retrievalPipelineService;
    // 声明当前字段。
    private final ChatSessionService chatSessionService;
    // 声明当前字段。
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 描述 `query` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `InoteResponse` 的返回值。
     */
    // 应用当前注解。
    @Transactional
    // 处理当前代码结构。
    public InoteResponse query(ChatRequest request) {
        // 执行当前语句。
        String question = request.getQuestion().trim();
        // 执行当前语句。
        log.info("Processing query, sessionId={}, question={}", request.getSessionId(), question);

        // 执行当前语句。
        ChatSession session = resolveSession(request.getSessionId());
        // 处理当前代码结构。
        List<ChatMessage> history = session == null
                // 处理当前代码结构。
                ? List.of()
                // 执行当前语句。
                : chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        // 执行当前语句。
        RetrievalResult result = retrievalPipelineService.retrieve(question);
        // 执行当前语句。
        List<Document> relevantDocs = result.documents();
        // 执行当前语句。
        List<SourceReference> sources = extractSources(relevantDocs);

        // 执行当前语句。
        String answer;
        // 执行当前流程控制分支。
        if (relevantDocs.isEmpty()) {
            // 执行当前语句。
            log.warn("No relevant documents found for query: {}", question);
            // 执行当前语句。
            answer = "The current documents do not provide enough information to answer this question.";
        // 处理当前代码结构。
        } else {
            // 执行当前语句。
            String context = buildContext(relevantDocs);
            // 执行当前语句。
            Prompt prompt = buildPrompt(question, context, history);
            // 执行当前语句。
            answer = callModelWithFallback(prompt);
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        if (session != null) {
            // 执行当前语句。
            persistConversation(session, question, answer, history.isEmpty());
        // 结束当前代码块。
        }

        // 返回当前结果。
        return InoteResponse.builder()
                // 处理当前代码结构。
                .sessionId(session == null ? null : session.getId())
                // 处理当前代码结构。
                .answer(answer)
                // 处理当前代码结构。
                .sources(sources)
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `buildPrompt` 操作。
     *
     * @param question 输入参数 `question`。
     * @param context 输入参数 `context`。
     * @param history 输入参数 `history`。
     * @return 类型为 `Prompt` 的返回值。
     */
    // 处理当前代码结构。
    private Prompt buildPrompt(String question, String context, List<ChatMessage> history) {
        // 执行当前语句。
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        // 执行当前语句。
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));

        // 执行当前语句。
        List<Message> messages = new ArrayList<>();
        // 执行当前语句。
        messages.add(systemMessage);
        // 执行当前语句。
        messages.addAll(toPromptMessages(history));
        // 执行当前语句。
        messages.add(new UserMessage(question));
        // 返回当前结果。
        return new Prompt(messages);
    // 结束当前代码块。
    }

    /**
     * 描述 `toPromptMessages` 操作。
     *
     * @param history 输入参数 `history`。
     * @return 类型为 `List<Message>` 的返回值。
     */
    // 处理当前代码结构。
    private List<Message> toPromptMessages(List<ChatMessage> history) {
        // 执行当前语句。
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        // 返回当前结果。
        return history.subList(fromIndex, history.size()).stream()
                // 处理当前代码结构。
                .map(message -> "assistant".equalsIgnoreCase(message.getRole())
                        // 处理当前代码结构。
                        ? new AssistantMessage(message.getContent())
                        // 处理当前代码结构。
                        : new UserMessage(message.getContent()))
                // 处理当前代码结构。
                .map(Message.class::cast)
                // 执行当前语句。
                .toList();
    // 结束当前代码块。
    }

    /**
     * 描述 `buildContext` 操作。
     *
     * @param documents 输入参数 `documents`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String buildContext(List<Document> documents) {
        // 执行当前语句。
        StringBuilder context = new StringBuilder();
        // 执行当前流程控制分支。
        for (int i = 0; i < documents.size(); i++) {
            // 执行当前语句。
            Document doc = documents.get(i);
            // 执行当前语句。
            context.append("[Document ").append(i + 1).append("]\n");
            // 执行当前语句。
            context.append(doc.getText());
            // 执行当前语句。
            context.append("\n\n");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return context.toString().trim();
    // 结束当前代码块。
    }

    /**
     * 描述 `callModelWithFallback` 操作。
     *
     * @param prompt 输入参数 `prompt`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String callModelWithFallback(Prompt prompt) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, prompt);
            // 执行当前流程控制分支。
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                // 返回当前结果。
                return response.getResult().getOutput().getText();
            // 结束当前代码块。
            }
            // 返回当前结果。
            return "The model did not generate a valid answer.";
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.error("Both primary and fallback models failed", e);
            // 返回当前结果。
            return "The service is temporarily unavailable. Please try again later.";
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `extractSources` 操作。
     *
     * @param documents 输入参数 `documents`。
     * @return 类型为 `List<SourceReference>` 的返回值。
     */
    // 处理当前代码结构。
    private List<SourceReference> extractSources(List<Document> documents) {
        // 执行当前语句。
        Set<String> seenUrls = new HashSet<>();
        // 返回当前结果。
        return documents.stream()
                // 处理当前代码结构。
                .map(doc -> {
                    // 执行当前语句。
                    Map<String, Object> metadata = doc.getMetadata();
                    // 执行当前语句。
                    String fileName = metadata.getOrDefault("file_name", "unknown").toString();
                    // 执行当前语句。
                    String url = metadata.getOrDefault("file_url", "").toString();
                    // 返回当前结果。
                    return SourceReference.builder()
                            // 处理当前代码结构。
                            .fileName(fileName)
                            // 处理当前代码结构。
                            .url(url)
                            // 执行当前语句。
                            .build();
                // 处理当前代码结构。
                })
                // 处理当前代码结构。
                .filter(source -> {
                    // 执行当前流程控制分支。
                    if (seenUrls.contains(source.getUrl())) {
                        // 返回当前结果。
                        return false;
                    // 结束当前代码块。
                    }
                    // 执行当前语句。
                    seenUrls.add(source.getUrl());
                    // 返回当前结果。
                    return true;
                // 处理当前代码结构。
                })
                // 处理当前代码结构。
                .limit(3)
                // 执行当前语句。
                .toList();
    // 结束当前代码块。
    }

    /**
     * 描述 `resolveSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `ChatSession` 的返回值。
     */
    // 处理当前代码结构。
    private ChatSession resolveSession(String sessionId) {
        // 执行当前流程控制分支。
        if (!StringUtils.hasText(sessionId)) {
            // 返回当前结果。
            return null;
        // 结束当前代码块。
        }
        // 返回当前结果。
        return chatSessionService.getSessionEntity(sessionId.trim());
    // 结束当前代码块。
    }

    /**
     * 描述 `persistConversation` 操作。
     *
     * @param session 输入参数 `session`。
     * @param question 输入参数 `question`。
     * @param answer 输入参数 `answer`。
     * @param firstTurn 输入参数 `firstTurn`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void persistConversation(ChatSession session, String question, String answer, boolean firstTurn) {
        // 处理当前代码结构。
        chatMessageRepository.save(ChatMessage.builder()
                // 处理当前代码结构。
                .session(session)
                // 处理当前代码结构。
                .role("user")
                // 处理当前代码结构。
                .content(question)
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        chatMessageRepository.save(ChatMessage.builder()
                // 处理当前代码结构。
                .session(session)
                // 处理当前代码结构。
                .role("assistant")
                // 处理当前代码结构。
                .content(answer)
                // 执行当前语句。
                .build());

        // 执行当前流程控制分支。
        if (firstTurn && "New Session".equals(session.getTitle())) {
            // 执行当前语句。
            session.setTitle(buildSessionTitle(question));
        // 结束当前代码块。
        }
        // 执行当前语句。
        chatSessionService.touchSession(session);
    // 结束当前代码块。
    }

    /**
     * 描述 `buildSessionTitle` 操作。
     *
     * @param question 输入参数 `question`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String buildSessionTitle(String question) {
        // 执行当前语句。
        String normalized = question.replaceAll("\\s+", " ").trim();
        // 执行当前流程控制分支。
        if (normalized.length() <= 20) {
            // 返回当前结果。
            return normalized;
        // 结束当前代码块。
        }
        // 返回当前结果。
        return normalized.substring(0, 20) + "...";
    // 结束当前代码块。
    }
// 结束当前代码块。
}
