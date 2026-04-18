// 声明当前源文件所属包。
package com.inote.repository;

import com.inote.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 定义会话仓储接口，负责聊天会话的持久化查询。
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    // 调用 `findAllByOwnerIdOrderByUpdatedAtDesc` 完成当前步骤。
    List<ChatSession> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    // 调用 `findByIdAndOwnerId` 完成当前步骤。
    Optional<ChatSession> findByIdAndOwnerId(String id, String ownerId);

    // 调用 `existsByIdAndOwnerId` 完成当前步骤。
    boolean existsByIdAndOwnerId(String id, String ownerId);
}
