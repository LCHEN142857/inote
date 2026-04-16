// 声明当前源文件的包。
package com.inote.service;

import com.inote.model.dto.ChatMessageResponse;
import com.inote.model.dto.ChatSessionCreateRequest;
import com.inote.model.dto.ChatSessionResponse;
import com.inote.model.dto.ChatSessionSummaryResponse;
import com.inote.model.dto.ChatSessionUpdateRequest;
import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.User;
import com.inote.repository.ChatMessageRepository;
import com.inote.repository.ChatSessionRepository;
import com.inote.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class ChatSessionService {

    // 声明当前字段。
    private final ChatSessionRepository chatSessionRepository;
    // 声明当前字段。
    private final ChatMessageRepository chatMessageRepository;
    // 声明当前字段。
    private final CurrentUserService currentUserService;

    /**
     * 描述 `createSession` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `ChatSessionResponse` 的返回值。
     */
    // 应用当前注解。
    @Transactional
    // 处理当前代码结构。
    public ChatSessionResponse createSession(ChatSessionCreateRequest request) {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 处理当前代码结构。
        ChatSession session = ChatSession.builder()
                // 处理当前代码结构。
                .title(resolveTitle(request == null ? null : request.getTitle()))
                // 处理当前代码结构。
                .owner(user)
                // 执行当前语句。
                .build();
        // 执行当前语句。
        ChatSession saved = chatSessionRepository.save(session);
        // 返回当前结果。
        return toResponse(saved, List.of());
    // 结束当前代码块。
    }

    /**
     * 描述 `listSessions` 操作。
     *
     * @return 类型为 `List<ChatSessionSummaryResponse>` 的返回值。
     */
    // 应用当前注解。
    @Transactional(readOnly = true)
    // 处理当前代码结构。
    public List<ChatSessionSummaryResponse> listSessions() {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 执行当前语句。
        List<ChatSession> sessions = chatSessionRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId());
        // 执行当前语句。
        Map<String, Long> messageCounts = loadMessageCounts(sessions);

        // 返回当前结果。
        return sessions.stream()
                // 处理当前代码结构。
                .map(session -> ChatSessionSummaryResponse.builder()
                        // 处理当前代码结构。
                        .id(session.getId())
                        // 处理当前代码结构。
                        .title(session.getTitle())
                        // 处理当前代码结构。
                        .messageCount(messageCounts.getOrDefault(session.getId(), 0L))
                        // 处理当前代码结构。
                        .createdAt(session.getCreatedAt())
                        // 处理当前代码结构。
                        .updatedAt(session.getUpdatedAt())
                        // 处理当前代码结构。
                        .build())
                // 执行当前语句。
                .toList();
    // 结束当前代码块。
    }

    /**
     * 描述 `getSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `ChatSessionResponse` 的返回值。
     */
    // 应用当前注解。
    @Transactional(readOnly = true)
    // 处理当前代码结构。
    public ChatSessionResponse getSession(String sessionId) {
        // 执行当前语句。
        ChatSession session = getSessionEntity(sessionId);
        // 返回当前结果。
        return toResponse(session, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    // 结束当前代码块。
    }

    /**
     * 描述 `updateSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @param request 输入参数 `request`。
     * @return 类型为 `ChatSessionResponse` 的返回值。
     */
    // 应用当前注解。
    @Transactional
    // 处理当前代码结构。
    public ChatSessionResponse updateSession(String sessionId, ChatSessionUpdateRequest request) {
        // 执行当前语句。
        ChatSession session = getSessionEntity(sessionId);
        // 执行当前语句。
        session.setTitle(request.getTitle().trim());
        // 执行当前语句。
        ChatSession saved = chatSessionRepository.save(session);
        // 返回当前结果。
        return toResponse(saved, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    // 结束当前代码块。
    }

    /**
     * 描述 `deleteSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 无返回值。
     */
    // 应用当前注解。
    @Transactional
    // 处理当前代码结构。
    public void deleteSession(String sessionId) {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 执行当前流程控制分支。
        if (!chatSessionRepository.existsByIdAndOwnerId(sessionId, user.getId())) {
            // 抛出当前异常。
            throw new EntityNotFoundException("Session not found: " + sessionId);
        // 结束当前代码块。
        }
        // 执行当前语句。
        chatSessionRepository.deleteById(sessionId);
    // 结束当前代码块。
    }

    /**
     * 描述 `getSessionEntity` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `ChatSession` 的返回值。
     */
    // 应用当前注解。
    @Transactional(readOnly = true)
    // 处理当前代码结构。
    public ChatSession getSessionEntity(String sessionId) {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 返回当前结果。
        return chatSessionRepository.findByIdAndOwnerId(sessionId, user.getId())
                // 执行当前语句。
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
    // 结束当前代码块。
    }

    /**
     * 描述 `touchSession` 操作。
     *
     * @param session 输入参数 `session`。
     * @return 无返回值。
     */
    // 应用当前注解。
    @Transactional
    // 处理当前代码结构。
    public void touchSession(ChatSession session) {
        // 执行当前语句。
        chatSessionRepository.save(session);
    // 结束当前代码块。
    }

    /**
     * 描述 `loadMessageCounts` 操作。
     *
     * @param sessions 输入参数 `sessions`。
     * @return 类型为 `Map<String, Long>` 的返回值。
     */
    // 处理当前代码结构。
    private Map<String, Long> loadMessageCounts(List<ChatSession> sessions) {
        // 处理当前代码结构。
        List<String> sessionIds = sessions.stream()
                // 处理当前代码结构。
                .map(ChatSession::getId)
                // 执行当前语句。
                .toList();
        // 执行当前流程控制分支。
        if (sessionIds.isEmpty()) {
            // 返回当前结果。
            return Map.of();
        // 结束当前代码块。
        }

        // 返回当前结果。
        return chatMessageRepository.countBySessionIds(sessionIds).stream()
                // 处理当前代码结构。
                .collect(Collectors.toMap(
                        // 处理当前代码结构。
                        row -> (String) row[0],
                        // 处理当前代码结构。
                        row -> (Long) row[1]
                // 执行当前语句。
                ));
    // 结束当前代码块。
    }

    /**
     * 描述 `toResponse` 操作。
     *
     * @param session 输入参数 `session`。
     * @param messages 输入参数 `messages`。
     * @return 类型为 `ChatSessionResponse` 的返回值。
     */
    // 处理当前代码结构。
    private ChatSessionResponse toResponse(ChatSession session, List<ChatMessage> messages) {
        // 返回当前结果。
        return ChatSessionResponse.builder()
                // 处理当前代码结构。
                .id(session.getId())
                // 处理当前代码结构。
                .title(session.getTitle())
                // 处理当前代码结构。
                .createdAt(session.getCreatedAt())
                // 处理当前代码结构。
                .updatedAt(session.getUpdatedAt())
                // 处理当前代码结构。
                .messages(messages.stream()
                        // 处理当前代码结构。
                        .map(this::toMessageResponse)
                        // 处理当前代码结构。
                        .toList())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `toMessageResponse` 操作。
     *
     * @param message 输入参数 `message`。
     * @return 类型为 `ChatMessageResponse` 的返回值。
     */
    // 处理当前代码结构。
    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        // 返回当前结果。
        return ChatMessageResponse.builder()
                // 处理当前代码结构。
                .id(message.getId())
                // 处理当前代码结构。
                .role(message.getRole())
                // 处理当前代码结构。
                .content(message.getContent())
                // 处理当前代码结构。
                .createdAt(message.getCreatedAt())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `resolveTitle` 操作。
     *
     * @param title 输入参数 `title`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String resolveTitle(String title) {
        // 执行当前流程控制分支。
        if (!StringUtils.hasText(title)) {
            // 返回当前结果。
            return "New Session";
        // 结束当前代码块。
        }
        // 返回当前结果。
        return title.trim();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
