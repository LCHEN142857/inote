// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 声明当前类提供 REST 风格接口。
@RestController
// 声明当前控制器的统一请求路径前缀。
@RequestMapping("/api/v1/chat")
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义问答接口控制器，负责聊天问答和会话管理接口。
public class ChatController {

    // 声明问答service变量，供后续流程使用。
    private final ChatService chatService;
    // 声明问答会话service变量，供后续流程使用。
    private final ChatSessionService chatSessionService;

    /**
     * 接收问答请求并返回模型回答结果。
     * @param request 请求参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 POST 请求。
    @PostMapping("/query")
    public ResponseEntity<InoteResponse> query(@Valid @RequestBody ChatRequest request) {
        // 计算并保存会话id结果。
        log.info("Received chat query, sessionId={}", request.getSessionId());
        // 返回成功响应。
        return ResponseEntity.ok(chatService.query(request));
    }

    // 声明当前方法处理 POST 请求。
    @PostMapping("/sessions")
    // 围绕响应entity问答补充当前业务语句。
    public ResponseEntity<ChatSessionResponse> createSession(@RequestBody(required = false) ChatSessionCreateRequest request) {
        // 按指定状态码返回响应。
        return ResponseEntity.status(HttpStatus.CREATED).body(chatSessionService.createSession(request));
    }

    /**
     * 返回当前用户的会话列表。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionSummaryResponse>> listSessions() {
        // 返回成功响应。
        return ResponseEntity.ok(chatSessionService.listSessions());
    }

    /**
     * 返回指定会话的完整详情。
     * @param sessionId 会话id参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable String sessionId) {
        // 返回成功响应。
        return ResponseEntity.ok(chatSessionService.getSession(sessionId));
    }

    /**
     * 更新指定会话的标题。
     * @param sessionId 会话id参数。
     * @param request 请求参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 PUT 请求。
    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> updateSession(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatSessionUpdateRequest request) {
        // 返回成功响应。
        return ResponseEntity.ok(chatSessionService.updateSession(sessionId, request));
    }

    /**
     * 删除指定会话。
     * @param sessionId 会话id参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 DELETE 请求。
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        // 删除当前持久化数据。
        chatSessionService.deleteSession(sessionId);
        // 返回无内容响应。
        return ResponseEntity.noContent().build();
    }
}
