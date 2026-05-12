package com.inote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.ai.chat.messages.SystemMessage;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are an enterprise knowledge-base assistant.

            Rules:
            1. Answer only from the provided reference documents.
            2. If the documents do not contain the answer, say that the current documents do not provide enough information.
            3. When chat history exists, use it to understand follow-up questions.
            4. Keep the answer concise and accurate.
            5. When citing or referring to evidence, use the file name provided in the reference documents.
            6. Never use placeholder labels such as "Document 1", "[Document 1]", "Source 1", or similar internal numbering in the final answer.

            Reference documents:
            {context}
            """;

    private static final String GENERAL_PROMPT = """
            You are a helpful assistant.

            Rules:
            1. Answer the user's question using your general knowledge.
            2. When chat history exists, use it to understand follow-up questions.
            3. Keep the answer concise and accurate.
            """;

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ChatModel chatModel;
    private final FallbackChatModel fallbackChatModel;
    private final RetrievalPipelineService retrievalPipelineService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserSettingsService userSettingsService;
    private final ChatModelSelectionService chatModelSelectionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InoteResponse query(ChatRequest request) {
        String question = request.getQuestion().trim();
        String modelName = chatModelSelectionService.resolveModel(request.getModel());
        log.info("Processing query, sessionId={}, model={}, question={}",
                request.getSessionId(), modelName, question);

        ChatSession session = resolveSession(request.getSessionId());
        List<ChatMessage> history = session == null
                ? List.of()
                : chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        RetrievalResult result = retrievalPipelineService.retrieve(question, modelName);
        List<Document> relevantDocs = result.documents();
        List<SourceReference> sources = extractSources(relevantDocs);

        String answer;
        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found for query: {}", question);
            if (userSettingsService.answerFromReferencesOnly()) {
                answer = "当前文档信息不足，无法回答这个问题。";
            } else {
                answer = callModelWithFallback(buildGeneralPrompt(question, history, modelName));
            }
        } else {
            String context = buildContext(relevantDocs);
            Prompt prompt = buildPrompt(question, context, history, modelName);
            answer = callModelWithFallback(prompt);
        }

        if (session != null) {
            persistConversation(session, question, answer, sources, history.isEmpty());
        }

        return InoteResponse.builder()
                .sessionId(session == null ? null : session.getId())
                .answer(answer)
                .sources(sources)
                .build();
    }

    private Prompt buildPrompt(String question, String context, List<ChatMessage> history, String modelName) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));

        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.addAll(toPromptMessages(history));
        messages.add(new UserMessage(question));
        return new Prompt(messages, chatModelSelectionService.buildOptions(modelName));
    }

    private Prompt buildGeneralPrompt(String question, List<ChatMessage> history, String modelName) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(GENERAL_PROMPT));
        messages.addAll(toPromptMessages(history));
        messages.add(new UserMessage(question));
        return new Prompt(messages, chatModelSelectionService.buildOptions(modelName));
    }

    private List<Message> toPromptMessages(List<ChatMessage> history) {
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        return history.subList(fromIndex, history.size()).stream()
                .map(message -> "assistant".equalsIgnoreCase(message.getRole())
                        ? new AssistantMessage(message.getContent())
                        : new UserMessage(message.getContent()))
                .map(Message.class::cast)
                .toList();
    }

    private String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (Document doc : documents) {
            String fileName = extractFileName(doc);
            context.append("<reference>\n");
            context.append("file_name: ").append(fileName).append("\n");
            context.append("content:\n");
            context.append(doc.getText()).append("\n");
            context.append("</reference>\n\n");
        }
        return context.toString().trim();
    }

    private String callModelWithFallback(Prompt prompt) {
        try {
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, prompt);
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            return "The model did not generate a valid answer.";
        } catch (Exception e) {
            log.error("Both primary and fallback models failed", e);
            return "The service is temporarily unavailable. Please try again later.";
        }
    }

    private List<SourceReference> extractSources(List<Document> documents) {
        Set<String> seenUrls = new HashSet<>();
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();
                    String fileName = extractFileName(doc);
                    String url = metadata.getOrDefault("file_url", "").toString();
                    return SourceReference.builder()
                            .fileName(fileName)
                            .url(url)
                            .build();
                })
                .filter(source -> {
                    if (seenUrls.contains(source.getUrl())) {
                        return false;
                    }
                    seenUrls.add(source.getUrl());
                    return true;
                })
                .limit(3)
                .toList();
    }

    private String extractFileName(Document doc) {
        Object fileName = doc.getMetadata().get("file_name");
        if (fileName == null) {
            return "unknown";
        }
        String value = fileName.toString().trim();
        return value.isEmpty() ? "unknown" : value;
    }

    private ChatSession resolveSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        return chatSessionService.getSessionEntity(sessionId.trim());
    }

    private void persistConversation(
            ChatSession session,
            String question,
            String answer,
            List<SourceReference> sources,
            boolean firstTurn
    ) {
        chatMessageRepository.save(ChatMessage.builder()
                .session(session)
                .role("user")
                .content(question)
                .build());

        chatMessageRepository.save(ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(answer)
                .sourcesJson(serializeSources(sources))
                .build());

        if (firstTurn && "New Session".equals(session.getTitle())) {
            session.setTitle(buildSessionTitle(question));
        }
        chatSessionService.touchSession(session);
    }

    private String serializeSources(List<SourceReference> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize chat sources", e);
            return null;
        }
    }

    private String buildSessionTitle(String question) {
        String normalized = question.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 20) {
            return normalized;
        }
        return normalized.substring(0, 20) + "...";
    }
}
