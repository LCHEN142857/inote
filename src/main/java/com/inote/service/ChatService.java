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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
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
            2. Start the answer with: 根据参考文件 {sourceNames}，then provide the answer.
            3. Synthesize all relevant reference chunks into a fluent, structured answer; do not copy only one matching sentence when the references contain broader context.
            4. When chat history exists, use it to understand follow-up questions.
            5. If the documents do not contain enough information, clearly say that the current documents do not provide enough information.
            6. When citing or referring to evidence, use the file name provided in the reference documents.
            7. Never use placeholder labels such as "Document 1", "[Document 1]", "Source 1", or similar internal numbering in the final answer.

            Reference documents:
            {context}
            """;

    private static final String REFERENCE_ASSISTED_PROMPT = """
            You are a helpful assistant with access to optional reference documents.

            Rules:
            1. Prefer the provided reference documents when they are relevant to the user's question.
            2. If the references are useful, summarize and polish them into a fluent, structured answer.
            3. Do not start the answer with "根据参考文件".
            4. If you use information outside the references, explicitly distinguish it from the uploaded documents.
            5. When chat history exists, use it to understand follow-up questions.
            6. Never use placeholder labels such as "Document 1", "[Document 1]", "Source 1", or similar internal numbering in the final answer.

            Reference documents:
            {context}
            """;

    private static final String GENERAL_PROMPT = """
            You are a helpful assistant.

            Rules:
            1. Answer the user's question using your general knowledge.
            2. Start the answer by saying: 未匹配到可用参考文档，以下基于通用知识回答：
            3. When chat history exists, use it to understand follow-up questions.
            4. Keep the answer concise, accurate, and structured.
            """;

    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int SOURCE_PREVIEW_LENGTH = 180;
    private static final long STREAM_TIMEOUT_MILLIS = 180_000L;

    private final ChatModel chatModel;
    private final FallbackChatModel fallbackChatModel;
    private final RetrievalPipelineService retrievalPipelineService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserSettingsService userSettingsService;
    private final ChatModelSelectionService chatModelSelectionService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

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
                Prompt prompt = buildGeneralPrompt(question, history, modelName);
                answer = callModelWithFallback(prompt, modelName);
            }
        } else {
            String context = buildContext(relevantDocs);
            Prompt prompt = buildReferencePrompt(question, context, history, modelName, sourceNames(sources));
            answer = callModelWithFallback(prompt, modelName);
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

    public SseEmitter streamQuery(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        String question = request.getQuestion().trim();
        String modelName = chatModelSelectionService.resolveModel(request.getModel());
        log.info("Processing streaming query, sessionId={}, model={}, question={}",
                request.getSessionId(), modelName, question);

        ChatSession session = resolveSession(request.getSessionId());
        List<ChatMessage> history = session == null
                ? List.of()
                : chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        RetrievalResult result = retrievalPipelineService.retrieve(question, modelName);
        List<Document> relevantDocs = result.documents();
        List<SourceReference> sources = extractSources(relevantDocs);

        sendStreamEvent(emitter, "metadata", InoteResponse.builder()
                .sessionId(session == null ? null : session.getId())
                .answer("")
                .sources(sources)
                .build());

        if (relevantDocs.isEmpty() && userSettingsService.answerFromReferencesOnly()) {
            String answer = "当前文档信息不足，无法回答这个问题。";
            sendStreamEvent(emitter, "delta", answer);
            completeStream(emitter, session, question, answer, sources, history.isEmpty());
            return emitter;
        }

        Prompt prompt;
        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found for streaming query: {}", question);
            prompt = buildGeneralPrompt(question, history, modelName);
        } else {
            prompt = buildReferencePrompt(question, buildContext(relevantDocs), history, modelName, sourceNames(sources));
        }

        StringBuilder answerBuilder = new StringBuilder();
        Flux<ChatResponse> stream = fallbackChatModel.streamWithFallback(chatModel, withModel(prompt, modelName));
        stream.subscribe(
                response -> {
                    String delta = extractText(response);
                    if (StringUtils.hasText(delta)) {
                        answerBuilder.append(delta);
                        sendStreamEvent(emitter, "delta", delta);
                    }
                },
                error -> {
                    log.error("Both primary and fallback model streams failed", error);
                    String message = "The service is temporarily unavailable. Please try again later.";
                    if (answerBuilder.isEmpty()) {
                        answerBuilder.append(message);
                        sendStreamEvent(emitter, "delta", message);
                    }
                    completeStream(emitter, session, question, answerBuilder.toString(), sources, history.isEmpty());
                },
                () -> completeStream(emitter, session, question, answerBuilder.toString(), sources, history.isEmpty())
        );

        return emitter;
    }

    private Prompt buildReferencePrompt(String question, String context, List<ChatMessage> history, String modelName, String sourceNames) {
        String template = userSettingsService.answerFromReferencesOnly() ? SYSTEM_PROMPT : REFERENCE_ASSISTED_PROMPT;
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(template);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of(
                "context", context,
                "sourceNames", sourceNames
        ));

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

    private String callModelWithFallback(Prompt prompt, String modelName) {
        try {
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, withModel(prompt, modelName));
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            return "The model did not generate a valid answer.";
        } catch (Exception e) {
            log.error("Both primary and fallback models failed", e);
            return "The service is temporarily unavailable. Please try again later.";
        }
    }

    private Prompt withModel(Prompt prompt, String modelName) {
        return new Prompt(prompt.getInstructions(), chatModelSelectionService.buildOptions(modelName));
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
                            .preview(buildPreview(doc.getText()))
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

    private String sourceNames(List<SourceReference> sources) {
        return sources.stream()
                .map(SourceReference::getFileName)
                .filter(StringUtils::hasText)
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("参考文档");
    }

    private String buildPreview(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SOURCE_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SOURCE_PREVIEW_LENGTH) + "...";
    }

    private String extractText(ChatResponse response) {
        if (response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private void sendStreamEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send chat stream event", e);
        }
    }

    private void completeStream(
            SseEmitter emitter,
            ChatSession session,
            String question,
            String answer,
            List<SourceReference> sources,
            boolean firstTurn
    ) {
        try {
            if (session != null) {
                transactionTemplate.executeWithoutResult(status -> persistConversation(session, question, answer, sources, firstTurn));
            }
            sendStreamEvent(emitter, "done", InoteResponse.builder()
                    .sessionId(session == null ? null : session.getId())
                    .answer(answer)
                    .sources(sources)
                    .build());
            emitter.complete();
        } catch (Exception e) {
            log.error("Failed to complete chat stream", e);
            emitter.completeWithError(e);
        }
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
