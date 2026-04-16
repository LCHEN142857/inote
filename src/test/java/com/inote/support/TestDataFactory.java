package com.inote.support;

import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;

import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * 创建测试用户实体。
     * @param id 用户主键。
     * @param username 用户名。
     * @param authToken 认证令牌。
     * @return 用户实体。
     * @throws RuntimeException 当构造过程出现未预期错误时抛出。
     */
    public static User user(String id, String username, String authToken) {
        // 返回带有默认密码和时间字段的用户实体。
        return User.builder().id(id).username(username).passwordHash("encoded-password").authToken(authToken).createdAt(LocalDateTime.of(2026, 4, 16, 10, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 10, 0)).build();
    }

    /**
     * 创建测试会话实体。
     * @param id 会话主键。
     * @param owner 会话所属用户。
     * @param title 会话标题。
     * @return 会话实体。
     * @throws RuntimeException 当构造过程出现未预期错误时抛出。
     */
    public static ChatSession session(String id, User owner, String title) {
        // 返回带有固定时间字段的会话实体。
        return ChatSession.builder().id(id).owner(owner).title(title).createdAt(LocalDateTime.of(2026, 4, 16, 11, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0)).build();
    }

    /**
     * 创建测试消息实体。
     * @param id 消息主键。
     * @param session 所属会话。
     * @param role 消息角色。
     * @param content 消息内容。
     * @return 消息实体。
     * @throws RuntimeException 当构造过程出现未预期错误时抛出。
     */
    public static ChatMessage message(String id, ChatSession session, String role, String content) {
        // 返回带有固定创建时间的消息实体。
        return ChatMessage.builder().id(id).session(session).role(role).content(content).createdAt(LocalDateTime.of(2026, 4, 16, 11, 5)).build();
    }

    /**
     * 创建测试文档实体。
     * @param id 文档主键。
     * @param owner 文档所属用户。
     * @param status 文档状态。
     * @return 文档实体。
     * @throws RuntimeException 当构造过程出现未预期错误时抛出。
     */
    public static Document document(String id, User owner, String status) {
        // 返回带有固定路径和时间字段的文档实体。
        return Document.builder().id(id).owner(owner).fileName("sample.txt").filePath("target/test-uploads/sample.txt").fileUrl("/api/v1/documents/files/" + id).contentType("text/plain").fileSize(32L).status(status).createdAt(LocalDateTime.of(2026, 4, 16, 12, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0)).build();
    }
}
