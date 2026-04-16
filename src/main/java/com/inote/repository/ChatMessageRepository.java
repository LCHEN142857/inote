// 声明当前源文件的包。
package com.inote.repository;

import com.inote.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 声明当前类型。
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 描述 `findBySessionIdOrderByCreatedAtAsc` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `List<ChatMessage>` 的返回值。
     */
    // 执行当前语句。
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 描述 `countBySessionId` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `long` 的返回值。
     */
    // 执行当前语句。
    long countBySessionId(String sessionId);

    /**
     * 描述 `countBySessionIds` 操作。
     *
     * @param sessionIds 输入参数 `sessionIds`。
     * @return 类型为 `List<Object[]>` 的返回值。
     */
    @Query("""
            select m.session.id, count(m)
            from ChatMessage m
            where m.session.id in :sessionIds
            group by m.session.id
            """)
    // 执行当前语句。
    List<Object[]> countBySessionIds(List<String> sessionIds);
// 结束当前代码块。
}
