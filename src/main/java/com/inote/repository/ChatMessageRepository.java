package com.inote.repository;

import com.inote.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    long countBySessionId(String sessionId);

    @Query("""
            select m.session.id, count(m)
            from ChatMessage m
            where m.session.id in :sessionIds
            group by m.session.id
            """)
    List<Object[]> countBySessionIds(List<String> sessionIds);
}
