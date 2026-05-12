package com.inote.support;

import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;

import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User user(String id, String username, String authToken) {
        return User.builder()
                .id(id)
                .username(username)
                .passwordHash("encoded-password")
                .authToken(authToken)
                .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                .build();
    }

    public static ChatSession session(String id, User owner, String title) {
        return ChatSession.builder()
                .id(id)
                .owner(owner)
                .title(title)
                .createdAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                .build();
    }

    public static ChatMessage message(String id, ChatSession session, String role, String content) {
        return ChatMessage.builder()
                .id(id)
                .session(session)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.of(2026, 4, 16, 11, 5))
                .build();
    }

    public static Document document(String id, User owner, String status) {
        return Document.builder()
                .id(id)
                .owner(owner)
                .fileName("sample.txt")
                .filePath("target/test-uploads/sample.txt")
                .fileUrl("/api/v1/documents/files/" + id)
                .contentType("text/plain")
                .fileSize(32L)
                .status(status)
                .active(Boolean.TRUE)
                .createdAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                .build();
    }
}
