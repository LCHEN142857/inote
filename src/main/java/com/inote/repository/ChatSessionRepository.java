package com.inote.repository;

import com.inote.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
