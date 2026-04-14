package com.inote.controller;

import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.ChatSessionCreateRequest;
import com.inote.model.dto.ChatSessionResponse;
import com.inote.model.dto.ChatSessionSummaryResponse;
import com.inote.model.dto.ChatSessionUpdateRequest;
import com.inote.model.dto.InoteResponse;
import com.inote.service.ChatService;
import com.inote.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;

    @PostMapping("/query")
    public ResponseEntity<InoteResponse> query(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat query, sessionId={}", request.getSessionId());
        return ResponseEntity.ok(chatService.query(request));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(@RequestBody(required = false) ChatSessionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatSessionService.createSession(request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionSummaryResponse>> listSessions() {
        return ResponseEntity.ok(chatSessionService.listSessions());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatSessionService.getSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> updateSession(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatSessionUpdateRequest request) {
        return ResponseEntity.ok(chatSessionService.updateSession(sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
