// 声明当前源文件所属包。
package com.inote.support;

import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;

import java.time.LocalDateTime;

// 定义测试数据工厂，用于快速创建测试实体和请求对象。
public final class TestDataFactory {

    /**
     * 处理testdatafactory相关逻辑。
     */
    private TestDataFactory() {
    }

    /**
     * 处理用户相关逻辑。
     * @param id id参数。
     * @param username username参数。
     * @param authToken 认证令牌参数。
     * @return 用户结果。
     */
    public static User user(String id, String username, String authToken) {
        // 返回组装完成的结果对象。
        return User.builder().id(id).username(username).passwordHash("encoded-password").authToken(authToken).createdAt(LocalDateTime.of(2026, 4, 16, 10, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 10, 0)).build();
    }

    /**
     * 处理会话相关逻辑。
     * @param id id参数。
     * @param owner 所属用户参数。
     * @param title 标题参数。
     * @return 匹配到的会话实体。
     */
    public static ChatSession session(String id, User owner, String title) {
        // 返回组装完成的结果对象。
        return ChatSession.builder().id(id).owner(owner).title(title).createdAt(LocalDateTime.of(2026, 4, 16, 11, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0)).build();
    }

    /**
     * 处理消息相关逻辑。
     * @param id id参数。
     * @param session 会话参数。
     * @param role role参数。
     * @param content 内容参数。
     * @return 问答消息结果。
     */
    public static ChatMessage message(String id, ChatSession session, String role, String content) {
        // 返回组装完成的结果对象。
        return ChatMessage.builder().id(id).session(session).role(role).content(content).createdAt(LocalDateTime.of(2026, 4, 16, 11, 5)).build();
    }

    /**
     * 处理文档相关逻辑。
     * @param id id参数。
     * @param owner 所属用户参数。
     * @param status 状态参数。
     * @return 匹配到的文档实体。
     */
    public static Document document(String id, User owner, String status) {
        // 返回组装完成的结果对象。
        return Document.builder().id(id).owner(owner).fileName("sample.txt").filePath("target/test-uploads/sample.txt").fileUrl("/api/v1/documents/files/" + id).contentType("text/plain").fileSize(32L).status(status).createdAt(LocalDateTime.of(2026, 4, 16, 12, 0)).updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0)).build();
    }
}
