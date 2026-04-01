package com.inote.controller;

import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.ChatResponse;
import com.inote.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 知识库问答
     * @param request 问答请求
     * @return 回答和来源
     */
    @PostMapping("/query")
    public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat query: {}", request.getQuestion());
        
        ChatResponse response = chatService.query(request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
