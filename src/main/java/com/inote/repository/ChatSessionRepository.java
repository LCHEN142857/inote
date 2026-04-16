package com.inote.repository;

import com.inote.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findAllByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<ChatSession> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByIdAndOwnerId(String id, String ownerId);
}
