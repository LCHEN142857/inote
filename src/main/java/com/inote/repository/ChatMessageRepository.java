// 声明当前源文件所属包。
package com.inote.repository;

import com.inote.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 定义消息仓储接口，负责聊天消息的持久化查询。
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    // 调用 `findBySessionIdOrderByCreatedAtAsc` 完成当前步骤。
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 调用 `countBySessionId` 完成当前步骤。
    long countBySessionId(String sessionId);

    // 应用 `Query` 注解声明当前行为。
    @Query("""
            select m.session.id, count(m)
            from ChatMessage m
            where m.session.id in :sessionIds
            group by m.session.id
            """)
    // 调用 `countBySessionIds` 完成当前步骤。
    List<Object[]> countBySessionIds(List<String> sessionIds);
}
