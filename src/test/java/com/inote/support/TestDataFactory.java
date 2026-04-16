// 声明当前源文件的包。
package com.inote.support;

import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;

import java.time.LocalDateTime;

// 处理当前代码结构。
public final class TestDataFactory {

    /**
     * 描述 `TestDataFactory` 操作。
     *
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    private TestDataFactory() {
    // 结束当前代码块。
    }

    /**
     * 描述 `user` 操作。
     *
     * @param id 输入参数 `id`。
     * @param username 输入参数 `username`。
     * @param authToken 输入参数 `authToken`。
     * @return 类型为 `User` 的返回值。
     */
    // 处理当前代码结构。
    public static User user(String id, String username, String authToken) {
        // 返回当前结果。
        return User.builder().id(id).username(username).passwordHash("encoded-password").authToken(authToken).createdAt(LocalDateTime.of(2026, 4, 16, 10, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 10, 0)).build();
    // 结束当前代码块。
    }

    /**
     * 描述 `session` 操作。
     *
     * @param id 输入参数 `id`。
     * @param owner 输入参数 `owner`。
     * @param title 输入参数 `title`。
     * @return 类型为 `ChatSession` 的返回值。
     */
    // 处理当前代码结构。
    public static ChatSession session(String id, User owner, String title) {
        // 返回当前结果。
        return ChatSession.builder().id(id).owner(owner).title(title).createdAt(LocalDateTime.of(2026, 4, 16, 11, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0)).build();
    // 结束当前代码块。
    }

    /**
     * 描述 `message` 操作。
     *
     * @param id 输入参数 `id`。
     * @param session 输入参数 `session`。
     * @param role 输入参数 `role`。
     * @param content 输入参数 `content`。
     * @return 类型为 `ChatMessage` 的返回值。
     */
    // 处理当前代码结构。
    public static ChatMessage message(String id, ChatSession session, String role, String content) {
        // 返回当前结果。
        return ChatMessage.builder().id(id).session(session).role(role).content(content).createdAt(LocalDateTime.of(2026, 4, 16, 11, 5)).build();
    // 结束当前代码块。
    }

    /**
     * 描述 `document` 操作。
     *
     * @param id 输入参数 `id`。
     * @param owner 输入参数 `owner`。
     * @param status 输入参数 `status`。
     * @return 类型为 `Document` 的返回值。
     */
    // 处理当前代码结构。
    public static Document document(String id, User owner, String status) {
        // 返回当前结果。
        return Document.builder().id(id).owner(owner).fileName("sample.txt").filePath("target/test-uploads/sample.txt").fileUrl("/api/v1/documents/files/" + id).contentType("text/plain").fileSize(32L).status(status).createdAt(LocalDateTime.of(2026, 4, 16, 12, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0)).build();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
