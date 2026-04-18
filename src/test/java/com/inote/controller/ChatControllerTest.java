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
    private UserRepository userRepository;

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

    @Test
    void createSessionAcceptsNullBody() throws Exception {
        when(chatSessionService.createSession(any())).thenReturn(sessionResponse("session-2", "New Session"));

        mockMvc.perform(post("/api/v1/chat/sessions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("session-2"))
                .andExpect(jsonPath("$.title").value("New Session"));
    }

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

    @Test
    void listSessionsReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        when(chatSessionService.listSessions()).thenThrow(new UnauthorizedException("Authentication required."));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    @Test
    void listSessionsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(chatSessionService.listSessions()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/chat/sessions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    @Test
    void getSessionReturnsSessionDetails() throws Exception {
        when(chatSessionService.getSession("session-1")).thenReturn(sessionResponse("session-1", "Detail"));

        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.messages[0].role").value("user"));
    }

    @Test
    void getSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        when(chatSessionService.getSession("missing"))
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        mockMvc.perform(get("/api/v1/chat/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    @Test
    void getSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(chatSessionService.getSession("session-1")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

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

    @Test
    void updateSessionReturnsBadRequestWhenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("title must not be blank"));
    }

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

    @Test
    void deleteSessionReturnsNoContent() throws Exception {
        doNothing().when(chatSessionService).deleteSession("session-1");

        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Session not found: missing"))
                .when(chatSessionService).deleteSession("missing");

        mockMvc.perform(delete("/api/v1/chat/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

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
