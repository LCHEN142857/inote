// 声明当前源文件的包。
package com.inote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inote.model.dto.ChatMessageResponse;
import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.ChatSessionCreateRequest;
import com.inote.model.dto.ChatSessionResponse;
import com.inote.model.dto.ChatSessionSummaryResponse;
import com.inote.model.dto.ChatSessionUpdateRequest;
import com.inote.model.dto.InoteResponse;
import com.inote.model.dto.SourceReference;
import com.inote.repository.UserRepository;
import com.inote.security.UnauthorizedException;
import com.inote.service.ChatService;
import com.inote.service.ChatSessionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 应用当前注解。
@WebMvcTest(ChatController.class)
// 应用当前注解。
@AutoConfigureMockMvc(addFilters = false)
// 声明当前类型。
class ChatControllerTest {

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private MockMvc mockMvc;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ObjectMapper objectMapper;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private ChatService chatService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private ChatSessionService chatSessionService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private UserRepository userRepository;

    /**
     * 描述 `queryReturnsAnswerWhenRequestIsValid` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryReturnsAnswerWhenRequestIsValid() throws Exception {
        // 处理当前代码结构。
        when(chatService.query(any(ChatRequest.class))).thenReturn(InoteResponse.builder()
                // 处理当前代码结构。
                .sessionId("session-1")
                // 处理当前代码结构。
                .answer("answer")
                // 处理当前代码结构。
                .sources(List.of(SourceReference.builder().fileName("doc.txt").url("/doc").build()))
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "sessionId", "session-1",
                                // 处理当前代码结构。
                                "question", "What is inote?"))))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                // 处理当前代码结构。
                .andExpect(jsonPath("$.answer").value("answer"))
                // 执行当前语句。
                .andExpect(jsonPath("$.sources[0].fileName").value("doc.txt"));
    // 结束当前代码块。
    }

    /**
     * 描述 `queryReturnsBadRequestWhenQuestionIsBlank` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryReturnsBadRequestWhenQuestionIsBlank() throws Exception {
        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "sessionId", "session-1",
                                // 处理当前代码结构。
                                "question", ""))))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 执行当前语句。
                .andExpect(jsonPath("$.question").value("question must not be blank"));
    // 结束当前代码块。
    }

    /**
     * 描述 `queryReturnsServerErrorWhenServiceThrowsUnexpectedException` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void queryReturnsServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
        // 执行当前语句。
        when(chatService.query(any(ChatRequest.class))).thenThrow(new RuntimeException("boom"));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "sessionId", "session-1",
                                // 处理当前代码结构。
                                "question", "What is inote?"))))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."))
                // 执行当前语句。
                .andExpect(jsonPath("$.sources").isArray());
    // 结束当前代码块。
    }

    /**
     * 描述 `createSessionReturnsCreatedResponse` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void createSessionReturnsCreatedResponse() throws Exception {
        // 执行当前语句。
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class))).thenReturn(sessionResponse("session-1", "My Session"));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                // 处理当前代码结构。
                .andExpect(status().isCreated())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.id").value("session-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.title").value("My Session"));
    // 结束当前代码块。
    }

    /**
     * 描述 `createSessionAcceptsNullBody` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void createSessionAcceptsNullBody() throws Exception {
        // 执行当前语句。
        when(chatSessionService.createSession(any())).thenReturn(sessionResponse("session-2", "New Session"));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON))
                // 处理当前代码结构。
                .andExpect(status().isCreated())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.id").value("session-2"))
                // 执行当前语句。
                .andExpect(jsonPath("$.title").value("New Session"));
    // 结束当前代码块。
    }

    /**
     * 描述 `createSessionReturnsUnauthorizedWhenServiceRejectsUser` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void createSessionReturnsUnauthorizedWhenServiceRejectsUser() throws Exception {
        // 处理当前代码结构。
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class)))
                // 执行当前语句。
                .thenThrow(new UnauthorizedException("Authentication required."));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                // 处理当前代码结构。
                .andExpect(status().isUnauthorized())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    // 结束当前代码块。
    }

    /**
     * 描述 `listSessionsReturnsAllSessions` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listSessionsReturnsAllSessions() throws Exception {
        // 处理当前代码结构。
        when(chatSessionService.listSessions()).thenReturn(List.of(
                // 处理当前代码结构。
                ChatSessionSummaryResponse.builder()
                        // 处理当前代码结构。
                        .id("session-1")
                        // 处理当前代码结构。
                        .title("First")
                        // 处理当前代码结构。
                        .messageCount(2)
                        // 处理当前代码结构。
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                        // 处理当前代码结构。
                        .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                        // 执行当前语句。
                        .build()));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$[0].id").value("session-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$[0].messageCount").value(2));
    // 结束当前代码块。
    }

    /**
     * 描述 `listSessionsReturnsUnauthorizedWhenNoCurrentUserExists` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listSessionsReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        // 执行当前语句。
        when(chatSessionService.listSessions()).thenThrow(new UnauthorizedException("Authentication required."));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 处理当前代码结构。
                .andExpect(status().isUnauthorized())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    // 结束当前代码块。
    }

    /**
     * 描述 `listSessionsReturnsServerErrorWhenUnexpectedFailureHappens` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listSessionsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 执行当前语句。
        when(chatSessionService.listSessions()).thenThrow(new RuntimeException("boom"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    // 结束当前代码块。
    }

    /**
     * 描述 `getSessionReturnsSessionDetails` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getSessionReturnsSessionDetails() throws Exception {
        // 执行当前语句。
        when(chatSessionService.getSession("session-1")).thenReturn(sessionResponse("session-1", "Detail"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.id").value("session-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.messages[0].role").value("user"));
    // 结束当前代码块。
    }

    /**
     * 描述 `getSessionReturnsNotFoundWhenSessionDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 处理当前代码结构。
        when(chatSessionService.getSession("missing"))
                // 执行当前语句。
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions/missing"))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    // 结束当前代码块。
    }

    /**
     * 描述 `getSessionReturnsServerErrorWhenUnexpectedFailureHappens` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 执行当前语句。
        when(chatSessionService.getSession("session-1")).thenThrow(new RuntimeException("boom"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    // 结束当前代码块。
    }

    /**
     * 描述 `updateSessionReturnsUpdatedSession` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void updateSessionReturnsUpdatedSession() throws Exception {
        // 处理当前代码结构。
        when(chatSessionService.updateSession(eq("session-1"), any(ChatSessionUpdateRequest.class)))
                // 执行当前语句。
                .thenReturn(sessionResponse("session-1", "Updated"));

        // 处理当前代码结构。
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 执行当前语句。
                .andExpect(jsonPath("$.title").value("Updated"));
    // 结束当前代码块。
    }

    /**
     * 描述 `updateSessionReturnsBadRequestWhenTitleIsBlank` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void updateSessionReturnsBadRequestWhenTitleIsBlank() throws Exception {
        // 处理当前代码结构。
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 执行当前语句。
                .andExpect(jsonPath("$.title").value("title must not be blank"));
    // 结束当前代码块。
    }

    /**
     * 描述 `updateSessionReturnsNotFoundWhenSessionDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void updateSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 处理当前代码结构。
        when(chatSessionService.updateSession(eq("missing"), any(ChatSessionUpdateRequest.class)))
                // 执行当前语句。
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        // 处理当前代码结构。
        mockMvc.perform(put("/api/v1/chat/sessions/missing")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    // 结束当前代码块。
    }

    /**
     * 描述 `deleteSessionReturnsNoContent` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void deleteSessionReturnsNoContent() throws Exception {
        // 执行当前语句。
        doNothing().when(chatSessionService).deleteSession("session-1");

        // 处理当前代码结构。
        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                // 执行当前语句。
                .andExpect(status().isNoContent());
    // 结束当前代码块。
    }

    /**
     * 描述 `deleteSessionReturnsNotFoundWhenSessionDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void deleteSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 处理当前代码结构。
        doThrow(new EntityNotFoundException("Session not found: missing"))
                // 执行当前语句。
                .when(chatSessionService).deleteSession("missing");

        // 处理当前代码结构。
        mockMvc.perform(delete("/api/v1/chat/sessions/missing"))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    // 结束当前代码块。
    }

    /**
     * 描述 `deleteSessionReturnsServerErrorWhenUnexpectedFailureHappens` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void deleteSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 处理当前代码结构。
        doThrow(new RuntimeException("boom"))
                // 执行当前语句。
                .when(chatSessionService).deleteSession("session-1");

        // 处理当前代码结构。
        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    // 结束当前代码块。
    }

    /**
     * 描述 `sessionResponse` 操作。
     *
     * @param id 输入参数 `id`。
     * @param title 输入参数 `title`。
     * @return 类型为 `ChatSessionResponse` 的返回值。
     */
    // 处理当前代码结构。
    private ChatSessionResponse sessionResponse(String id, String title) {
        // 返回当前结果。
        return ChatSessionResponse.builder()
                // 处理当前代码结构。
                .id(id)
                // 处理当前代码结构。
                .title(title)
                // 处理当前代码结构。
                .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                // 处理当前代码结构。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                // 处理当前代码结构。
                .messages(List.of(ChatMessageResponse.builder()
                        // 处理当前代码结构。
                        .id("message-1")
                        // 处理当前代码结构。
                        .role("user")
                        // 处理当前代码结构。
                        .content("hello")
                        // 处理当前代码结构。
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 1))
                        // 处理当前代码结构。
                        .build()))
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
