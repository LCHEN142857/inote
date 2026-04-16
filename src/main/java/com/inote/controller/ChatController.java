// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 应用当前注解。
@RestController
// 应用当前注解。
@RequestMapping("/api/v1/chat")
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class ChatController {

    // 声明当前字段。
    private final ChatService chatService;
    // 声明当前字段。
    private final ChatSessionService chatSessionService;

    /**
     * 描述 `query` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `ResponseEntity<InoteResponse>` 的返回值。
     */
    // 应用当前注解。
    @PostMapping("/query")
    // 处理当前代码结构。
    public ResponseEntity<InoteResponse> query(@Valid @RequestBody ChatRequest request) {
        // 执行当前语句。
        log.info("Received chat query, sessionId={}", request.getSessionId());
        // 返回当前结果。
        return ResponseEntity.ok(chatService.query(request));
    // 结束当前代码块。
    }

    /**
     * 描述 `createSession` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `ResponseEntity<ChatSessionResponse>` 的返回值。
     */
    // 应用当前注解。
    @PostMapping("/sessions")
    // 处理当前代码结构。
    public ResponseEntity<ChatSessionResponse> createSession(@RequestBody(required = false) ChatSessionCreateRequest request) {
        // 返回当前结果。
        return ResponseEntity.status(HttpStatus.CREATED).body(chatSessionService.createSession(request));
    // 结束当前代码块。
    }

    /**
     * 描述 `listSessions` 操作。
     *
     * @return 类型为 `ResponseEntity<List<ChatSessionSummaryResponse>>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/sessions")
    // 处理当前代码结构。
    public ResponseEntity<List<ChatSessionSummaryResponse>> listSessions() {
        // 返回当前结果。
        return ResponseEntity.ok(chatSessionService.listSessions());
    // 结束当前代码块。
    }

    /**
     * 描述 `getSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `ResponseEntity<ChatSessionResponse>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/sessions/{sessionId}")
    // 处理当前代码结构。
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable String sessionId) {
        // 返回当前结果。
        return ResponseEntity.ok(chatSessionService.getSession(sessionId));
    // 结束当前代码块。
    }

    /**
     * 描述 `updateSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @param request 输入参数 `request`。
     * @return 类型为 `ResponseEntity<ChatSessionResponse>` 的返回值。
     */
    // 应用当前注解。
    @PutMapping("/sessions/{sessionId}")
    // 处理当前代码结构。
    public ResponseEntity<ChatSessionResponse> updateSession(
            // 应用当前注解。
            @PathVariable String sessionId,
            // 应用当前注解。
            @Valid @RequestBody ChatSessionUpdateRequest request) {
        // 返回当前结果。
        return ResponseEntity.ok(chatSessionService.updateSession(sessionId, request));
    // 结束当前代码块。
    }

    /**
     * 描述 `deleteSession` 操作。
     *
     * @param sessionId 输入参数 `sessionId`。
     * @return 类型为 `ResponseEntity<Void>` 的返回值。
     */
    // 应用当前注解。
    @DeleteMapping("/sessions/{sessionId}")
    // 处理当前代码结构。
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        // 执行当前语句。
        chatSessionService.deleteSession(sessionId);
        // 返回当前结果。
        return ResponseEntity.noContent().build();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
