// 声明当前源文件所属包。
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

// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义会话服务，负责聊天会话的增删改查。
public class ChatSessionService {

    // 声明问答会话repository变量，供后续流程使用。
    private final ChatSessionRepository chatSessionRepository;
    // 声明问答消息repository变量，供后续流程使用。
    private final ChatMessageRepository chatMessageRepository;
    // 声明当前用户service变量，供后续流程使用。
    private final CurrentUserService currentUserService;

    /**
     * 为当前用户创建新的聊天会话。
     * @param request 请求参数。
     * @return 问答会话响应结果。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional
    public ChatSessionResponse createSession(ChatSessionCreateRequest request) {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 围绕问答会话会话补充当前业务语句。
        ChatSession session = ChatSession.builder()
                // 设置标题字段的取值。
                .title(resolveTitle(request == null ? null : request.getTitle()))
                // 设置所属用户字段的取值。
                .owner(user)
                // 完成当前建造者对象的组装。
                .build();
        // 保存saved对象。
        ChatSession saved = chatSessionRepository.save(session);
        // 返回 `toResponse` 的处理结果。
        return toResponse(saved, List.of());
    }

    /**
     * 查询当前用户的会话列表和消息数量。
     * @return 列表形式的处理结果。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> listSessions() {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 查询会话数据。
        List<ChatSession> sessions = chatSessionRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId());
        // 计算并保存消息counts结果。
        Map<String, Long> messageCounts = loadMessageCounts(sessions);

        // 返回 `stream` 的处理结果。
        return sessions.stream()
                // 设置map字段的取值。
                .map(session -> ChatSessionSummaryResponse.builder()
                        // 设置id字段的取值。
                        .id(session.getId())
                        // 设置标题字段的取值。
                        .title(session.getTitle())
                        // 设置消息count字段的取值。
                        .messageCount(messageCounts.getOrDefault(session.getId(), 0L))
                        // 设置createdat字段的取值。
                        .createdAt(session.getCreatedAt())
                        // 设置updatedat字段的取值。
                        .updatedAt(session.getUpdatedAt())
                        // 完成当前建造者对象的组装。
                        .build())
                // 设置tolist字段的取值。
                .toList();
    }

    /**
     * 查询指定会话的详情和消息历史。
     * @param sessionId 会话id参数。
     * @return 问答会话响应结果。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(String sessionId) {
        // 计算并保存会话结果。
        ChatSession session = getSessionEntity(sessionId);
        // 返回 `toResponse` 的处理结果。
        return toResponse(session, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    /**
     * 更新指定会话的标题。
     * @param sessionId 会话id参数。
     * @param request 请求参数。
     * @return 问答会话响应结果。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional
    public ChatSessionResponse updateSession(String sessionId, ChatSessionUpdateRequest request) {
        // 计算并保存会话结果。
        ChatSession session = getSessionEntity(sessionId);
        // 更新标题字段。
        session.setTitle(request.getTitle().trim());
        // 保存saved对象。
        ChatSession saved = chatSessionRepository.save(session);
        // 返回 `toResponse` 的处理结果。
        return toResponse(saved, chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    /**
     * 删除当前用户拥有的指定会话。
     * @param sessionId 会话id参数。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional
    public void deleteSession(String sessionId) {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 根据条件判断当前分支是否执行。
        if (!chatSessionRepository.existsByIdAndOwnerId(sessionId, user.getId())) {
            // 抛出 `EntityNotFoundException` 异常中断当前流程。
            throw new EntityNotFoundException("Session not found: " + sessionId);
        }
        // 删除当前持久化数据。
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * 读取并校验当前用户拥有的会话实体。
     * @param sessionId 会话id参数。
     * @return 匹配到的会话实体。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional(readOnly = true)
    public ChatSession getSessionEntity(String sessionId) {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 返回 `findByIdAndOwnerId` 的处理结果。
        return chatSessionRepository.findByIdAndOwnerId(sessionId, user.getId())
                // 设置orelsethrow字段的取值。
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
    }

    /**
     * 刷新会话更新时间，表示会话刚被访问或写入。
     * @param session 会话参数。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional
    public void touchSession(ChatSession session) {
        // 保存当前对象到持久化层。
        chatSessionRepository.save(session);
    }

    /**
     * 处理load消息counts相关逻辑。
     * @param sessions 会话参数。
     * @return long>结果。
     */
    private Map<String, Long> loadMessageCounts(List<ChatSession> sessions) {
        // 围绕会话ids会话补充当前业务语句。
        List<String> sessionIds = sessions.stream()
                // 设置map字段的取值。
                .map(ChatSession::getId)
                // 设置tolist字段的取值。
                .toList();
        // 根据条件判断当前分支是否执行。
        if (sessionIds.isEmpty()) {
            // 返回固定映射结果。
            return Map.of();
        }

        // 返回 `countBySessionIds` 的处理结果。
        return chatMessageRepository.countBySessionIds(sessionIds).stream()
                // 设置collect字段的取值。
                .collect(Collectors.toMap(
                        // 围绕rowrow补充当前业务语句。
                        row -> (String) row[0],
                        // 围绕rowrow补充当前业务语句。
                        row -> (Long) row[1]
                // 继续补全当前链式调用或多行表达式。
                ));
    }

    /**
     * 处理to响应相关逻辑。
     * @param session 会话参数。
     * @param messages 消息参数。
     * @return 问答会话响应结果。
     */
    private ChatSessionResponse toResponse(ChatSession session, List<ChatMessage> messages) {
        // 返回组装完成的结果对象。
        return ChatSessionResponse.builder()
                // 设置id字段的取值。
                .id(session.getId())
                // 设置标题字段的取值。
                .title(session.getTitle())
                // 设置createdat字段的取值。
                .createdAt(session.getCreatedAt())
                // 设置updatedat字段的取值。
                .updatedAt(session.getUpdatedAt())
                // 设置消息字段的取值。
                .messages(messages.stream()
                        // 设置map字段的取值。
                        .map(this::toMessageResponse)
                        // 设置tolist字段的取值。
                        .toList())
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 处理to消息响应相关逻辑。
     * @param message 消息参数。
     * @return 问答消息响应结果。
     */
    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        // 返回组装完成的结果对象。
        return ChatMessageResponse.builder()
                // 设置id字段的取值。
                .id(message.getId())
                // 设置role字段的取值。
                .role(message.getRole())
                // 设置内容字段的取值。
                .content(message.getContent())
                // 设置createdat字段的取值。
                .createdAt(message.getCreatedAt())
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 处理resolve标题相关逻辑。
     * @param title 标题参数。
     * @return 处理后的字符串结果。
     */
    private String resolveTitle(String title) {
        // 根据条件判断当前分支是否执行。
        if (!StringUtils.hasText(title)) {
            // 返回"newsession"。
            return "New Session";
        }
        // 返回 `trim` 的处理结果。
        return title.trim();
    }
}
