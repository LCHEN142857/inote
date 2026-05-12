package com.inote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inote.model.dto.ChatMessageResponse;
import com.inote.model.dto.ChatModelCatalogResponse;
import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.ChatSessionCreateRequest;
import com.inote.model.dto.ChatSessionResponse;
import com.inote.model.dto.ChatSessionSummaryResponse;
import com.inote.model.dto.ChatSessionUpdateRequest;
import com.inote.model.dto.InoteResponse;
import com.inote.model.dto.SourceReference;
import com.inote.repository.UserRepository;
import com.inote.security.ReplayProtectionService;
import com.inote.security.RequestRateLimitService;
import com.inote.security.UnauthorizedException;
import com.inote.service.ChatModelSelectionService;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

// 验证聊天控制器的会话和问答接口在正常与异常条件下的行为。
@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ChatSessionService chatSessionService;

    @MockBean
    private ChatModelSelectionService chatModelSelectionService;

    @MockBean
    private ReplayProtectionService replayProtectionService;

    @MockBean
    private RequestRateLimitService requestRateLimitService;

    @MockBean
    private UserRepository userRepository;

    // 验证提问接口会返回答案和引用来源。
    @Test
    void queryReturnsAnswerWhenRequestIsValid() throws Exception {
        when(chatService.query(any(ChatRequest.class))).thenReturn(InoteResponse.builder()
                .sessionId("session-1")
                .answer("answer")
                .sources(List.of(SourceReference.builder().fileName("doc.txt").url("/doc").build()))
                .build());

        mockMvc.perform(post("/api/v1/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", "What is inote?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.answer").value("answer"))
                .andExpect(jsonPath("$.sources[0].fileName").value("doc.txt"));
    }

    // 验证问题为空时会被请求校验拦截。
    @Test
    void queryReturnsBadRequestWhenQuestionIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.question").value("question must not be blank"));
    }

    // 验证聊天服务抛出未知异常时控制器会返回通用错误。
    @Test
    void queryReturnsServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
        when(chatService.query(any(ChatRequest.class))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", "What is inote?"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    // 验证流式提问接口会返回 SSE emitter。
    @Test
    void streamQueryReturnsEmitterWhenRequestIsValid() throws Exception {
        when(chatService.streamQuery(any(ChatRequest.class))).thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/v1/chat/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", "What is inote?"))))
                .andExpect(status().isOk());
    }

    // 验证模型列表接口返回默认模型和可选模型。
    @Test
    void modelsReturnsConfiguredCatalog() throws Exception {
        when(chatModelSelectionService.catalog()).thenReturn(ChatModelCatalogResponse.builder()
                .defaultModel("qwen")
                .availableModels(List.of("qwen", "glm"))
                .build());

        mockMvc.perform(get("/api/v1/chat/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultModel").value("qwen"))
                .andExpect(jsonPath("$.availableModels[1]").value("glm"));
    }

    // 验证新建会话接口返回创建结果。
    @Test
    void createSessionReturnsCreatedResponse() throws Exception {
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class)))
                .thenReturn(sessionResponse("session-1", "My Session"));

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.title").value("My Session"));
    }

    // 验证新建会话接口可以接受空请求体。
    @Test
    void createSessionAcceptsNullBody() throws Exception {
        when(chatSessionService.createSession(any())).thenReturn(sessionResponse("session-2", "New Session"));

        mockMvc.perform(post("/api/v1/chat/sessions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-2"))
                .andExpect(jsonPath("$.title").value("New Session"));
    }

    // 验证未认证时创建会话会返回 401。
    @Test
    void createSessionReturnsUnauthorizedWhenServiceRejectsUser() throws Exception {
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class)))
                .thenThrow(new UnauthorizedException("Authentication required."));

        mockMvc.perform(post("/api/v1/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    // 验证会话列表接口返回所有会话摘要。
    @Test
    void listSessionsReturnsAllSessions() throws Exception {
        when(chatSessionService.listSessions()).thenReturn(List.of(
                ChatSessionSummaryResponse.builder()
                        .id("session-1")
                        .title("First")
                        .messageCount(2)
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                        .build()));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("session-1"))
                .andExpect(jsonPath("$[0].messageCount").value(2));
    }

    // 验证未认证时会话列表接口返回 401。
    @Test
    void listSessionsReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        when(chatSessionService.listSessions()).thenThrow(new UnauthorizedException("Authentication required."));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    // 验证未处理异常会被统一映射为 500。
    @Test
    void listSessionsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(chatSessionService.listSessions()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    // 验证会话详情接口返回消息列表。
    @Test
    void getSessionReturnsSessionDetails() throws Exception {
        when(chatSessionService.getSession("session-1")).thenReturn(sessionResponse("session-1", "Detail"));

        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.messages[0].role").value("user"));
    }

    // 验证不存在的会话会返回 404。
    @Test
    void getSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        when(chatSessionService.getSession("missing"))
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        mockMvc.perform(get("/api/v1/chat/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    // 验证会话详情接口的未知失败会返回 500。
    @Test
    void getSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(chatSessionService.getSession("session-1")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    // 验证更新会话接口返回更新后的标题。
    @Test
    void updateSessionReturnsUpdatedSession() throws Exception {
        when(chatSessionService.updateSession(eq("session-1"), any(ChatSessionUpdateRequest.class)))
                .thenReturn(sessionResponse("session-1", "Updated"));

        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    // 验证空标题会触发请求校验失败。
    @Test
    void updateSessionReturnsBadRequestWhenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("title must not be blank"));
    }

    // 验证更新不存在的会话会返回 404。
    @Test
    void updateSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        when(chatSessionService.updateSession(eq("missing"), any(ChatSessionUpdateRequest.class)))
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        mockMvc.perform(put("/api/v1/chat/sessions/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    // 验证删除会话接口返回 204。
    @Test
    void deleteSessionReturnsNoContent() throws Exception {
        doNothing().when(chatSessionService).deleteSession("session-1");

        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isNoContent());
    }

    // 验证删除不存在的会话会返回 404。
    @Test
    void deleteSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Session not found: missing"))
                .when(chatSessionService).deleteSession("missing");

        mockMvc.perform(delete("/api/v1/chat/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    // 验证删除接口的未知失败会返回 500。
    @Test
    void deleteSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(chatSessionService).deleteSession("session-1");

        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    private ChatSessionResponse sessionResponse(String id, String title) {
        return ChatSessionResponse.builder()
                .id(id)
                .title(title)
                .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                .messages(List.of(ChatMessageResponse.builder()
                        .id("message-1")
                        .role("user")
                        .content("hello")
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 1))
                        .build()))
                .build();
    }
}
