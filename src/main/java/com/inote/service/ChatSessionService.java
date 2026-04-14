package com.inote.service;

import com.inote.model.dto.ChatMessageResponse;
import com.inote.model.dto.ChatSessionCreateRequest;
import com.inote.model.dto.ChatSessionResponse;
import com.inote.model.dto.ChatSessionSummaryResponse;
import com.inote.model.dto.ChatSessionUpdateRequest;
import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.repository.ChatMessageRepository;
import com.inote.repository.ChatSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatSessionResponse createSession(ChatSessionCreateRequest request) {
        ChatSession session = ChatSession.builder()
                .title(resolveTitle(request == null ? null : request.getTitle()))
                .build();
        ChatSession saved = chatSessionRepository.save(session);
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> listSessions() {
        List<ChatSession> sessions = chatSessionRepository.findAllByOrderByUpdatedAtDesc();
        Map<String, Long> messageCounts = loadMessageCounts(sessions);

        return sessions.stream()
                .map(session -> ChatSessionSummaryResponse.builder()
                        .id(session.getId())
                        .title(session.getTitle())
                        .messageCount(messageCounts.getOrDefault(session.getId(), 0L))
                        .createdAt(session.getCreatedAt())
                        .updatedAt(session.getUpdatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(String sessionId) {
        ChatSession session = getSessionEntity(sessionId);
        return toResponse(session, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @Transactional
    public ChatSessionResponse updateSession(String sessionId, ChatSessionUpdateRequest request) {
        ChatSession session = getSessionEntity(sessionId);
        session.setTitle(request.getTitle().trim());
        ChatSession saved = chatSessionRepository.save(session);
        return toResponse(saved, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @Transactional
    public void deleteSession(String sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("Session not found: " + sessionId);
        }
        chatSessionRepository.deleteById(sessionId);
    }

    @Transactional(readOnly = true)
    public ChatSession getSessionEntity(String sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
    }

    @Transactional
    public void touchSession(ChatSession session) {
        chatSessionRepository.save(session);
    }

    private Map<String, Long> loadMessageCounts(List<ChatSession> sessions) {
        List<String> sessionIds = sessions.stream()
                .map(ChatSession::getId)
                .toList();
        if (sessionIds.isEmpty()) {
            return Map.of();
        }

        return chatMessageRepository.countBySessionIds(sessionIds).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    private ChatSessionResponse toResponse(ChatSession session, List<ChatMessage> messages) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messages(messages.stream()
                        .map(this::toMessageResponse)
                        .toList())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String resolveTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "New Session";
        }
        return title.trim();
    }
}
