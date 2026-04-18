// 声明当前源文件所属包。
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

// 应用 `WebMvcTest` 注解声明当前行为。
@WebMvcTest(ChatController.class)
// 在测试环境中注入 MockMvc。
@AutoConfigureMockMvc(addFilters = false)
// 定义 `ChatControllerTest` 类型。
class ChatControllerTest {

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明mockmvc变量，供后续流程使用。
    private MockMvc mockMvc;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明objectmapper变量，供后续流程使用。
    private ObjectMapper objectMapper;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明问答service变量，供后续流程使用。
    private ChatService chatService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明问答会话service变量，供后续流程使用。
    private ChatSessionService chatSessionService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    /**
     * 处理查询returns回答when请求isvalid相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryReturnsAnswerWhenRequestIsValid() throws Exception {
        // 定义当前类型。
        when(chatService.query(any(ChatRequest.class))).thenReturn(InoteResponse.builder()
                // 设置会话id字段的取值。
                .sessionId("session-1")
                // 设置回答字段的取值。
                .answer("answer")
                // 设置来源字段的取值。
                .sources(List.of(SourceReference.builder().fileName("doc.txt").url("/doc").build()))
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", "What is inote?"))))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("answer"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.sources[0].fileName").value("doc.txt"));
    }

    /**
     * 处理查询returnsbad请求when问题isblank相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryReturnsBadRequestWhenQuestionIsBlank() throws Exception {
        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", ""))))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.question").value("question must not be blank"));
    }

    /**
     * 处理查询returnsserver错误信息whenservicethrowsunexpectedexception相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void queryReturnsServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
        // 定义当前类型。
        when(chatService.query(any(ChatRequest.class))).thenThrow(new RuntimeException("boom"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/query")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sessionId", "session-1",
                                "question", "What is inote?"))))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.sources").isArray());
    }

    /**
     * 处理create会话returnscreated响应相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void createSessionReturnsCreatedResponse() throws Exception {
        // 定义当前类型。
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class))).thenReturn(sessionResponse("session-1", "My Session"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                // 继续校验接口响应结果。
                .andExpect(status().isCreated())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.id").value("session-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.title").value("My Session"));
    }

    /**
     * 处理create会话acceptsnullbody相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void createSessionAcceptsNullBody() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.createSession(any())).thenReturn(sessionResponse("session-2", "New Session"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON))
                // 继续校验接口响应结果。
                .andExpect(status().isCreated())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.id").value("session-2"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.title").value("New Session"));
    }

    /**
     * 处理create会话returnsunauthorizedwhenservicerejects用户相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void createSessionReturnsUnauthorizedWhenServiceRejectsUser() throws Exception {
        // 定义当前类型。
        when(chatSessionService.createSession(any(ChatSessionCreateRequest.class)))
                // 设置thenthrow字段的取值。
                .thenThrow(new UnauthorizedException("Authentication required."));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/chat/sessions")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                // 继续校验接口响应结果。
                .andExpect(status().isUnauthorized())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    /**
     * 处理list会话returnsall会话相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listSessionsReturnsAllSessions() throws Exception {
        // 围绕when问答会话补充当前业务语句。
        when(chatSessionService.listSessions()).thenReturn(List.of(
                // 围绕问答会话summary补充当前业务语句。
                ChatSessionSummaryResponse.builder()
                        // 设置id字段的取值。
                        .id("session-1")
                        // 设置标题字段的取值。
                        .title("First")
                        // 设置消息count字段的取值。
                        .messageCount(2)
                        // 设置createdat字段的取值。
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                        // 设置updatedat字段的取值。
                        .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                        // 完成当前建造者对象的组装。
                        .build()));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$[0].id").value("session-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$[0].messageCount").value(2));
    }

    /**
     * 处理list会话returnsunauthorizedwhenno当前用户exists相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listSessionsReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.listSessions()).thenThrow(new UnauthorizedException("Authentication required."));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 继续校验接口响应结果。
                .andExpect(status().isUnauthorized())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    /**
     * 处理list会话returnsserver错误信息whenunexpectedfailurehappens相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listSessionsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.listSessions()).thenThrow(new RuntimeException("boom"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions"))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    }

    /**
     * 处理get会话returns会话details相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getSessionReturnsSessionDetails() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.getSession("session-1")).thenReturn(sessionResponse("session-1", "Detail"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.id").value("session-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.messages[0].role").value("user"));
    }

    /**
     * 处理get会话returnsnotfoundwhen会话doesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 围绕when问答会话补充当前业务语句。
        when(chatSessionService.getSession("missing"))
                // 设置thenthrow字段的取值。
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions/missing"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    /**
     * 处理get会话returnsserver错误信息whenunexpectedfailurehappens相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(chatSessionService.getSession("session-1")).thenThrow(new RuntimeException("boom"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/chat/sessions/session-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    }

    /**
     * 处理update会话returnsupdated会话相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void updateSessionReturnsUpdatedSession() throws Exception {
        // 定义当前类型。
        when(chatSessionService.updateSession(eq("session-1"), any(ChatSessionUpdateRequest.class)))
                // 设置thenreturn字段的取值。
                .thenReturn(sessionResponse("session-1", "Updated"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    /**
     * 处理update会话returnsbad请求when标题isblank相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void updateSessionReturnsBadRequestWhenTitleIsBlank() throws Exception {
        // 发起当前接口的集成测试请求。
        mockMvc.perform(put("/api/v1/chat/sessions/session-1")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.title").value("title must not be blank"));
    }

    /**
     * 处理update会话returnsnotfoundwhen会话doesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void updateSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 定义当前类型。
        when(chatSessionService.updateSession(eq("missing"), any(ChatSessionUpdateRequest.class)))
                // 设置thenthrow字段的取值。
                .thenThrow(new EntityNotFoundException("Session not found: missing"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(put("/api/v1/chat/sessions/missing")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    /**
     * 处理delete会话returnsno内容相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void deleteSessionReturnsNoContent() throws Exception {
        // 删除当前持久化数据。
        doNothing().when(chatSessionService).deleteSession("session-1");

        // 发起当前接口的集成测试请求。
        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isNoContent());
    }

    /**
     * 处理delete会话returnsnotfoundwhen会话doesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void deleteSessionReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
        // 围绕dothrowentity补充当前业务语句。
        doThrow(new EntityNotFoundException("Session not found: missing"))
                // 设置when字段的取值。
                .when(chatSessionService).deleteSession("missing");

        // 发起当前接口的集成测试请求。
        mockMvc.perform(delete("/api/v1/chat/sessions/missing"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Session not found: missing"));
    }

    /**
     * 处理delete会话returnsserver错误信息whenunexpectedfailurehappens相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void deleteSessionReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 围绕dothrow运行时补充当前业务语句。
        doThrow(new RuntimeException("boom"))
                // 设置when字段的取值。
                .when(chatSessionService).deleteSession("session-1");

        // 发起当前接口的集成测试请求。
        mockMvc.perform(delete("/api/v1/chat/sessions/session-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    }

    /**
     * 处理会话响应相关逻辑。
     * @param id id参数。
     * @param title 标题参数。
     * @return 问答会话响应结果。
     */
    private ChatSessionResponse sessionResponse(String id, String title) {
        // 返回组装完成的结果对象。
        return ChatSessionResponse.builder()
                // 设置id字段的取值。
                .id(id)
                // 设置标题字段的取值。
                .title(title)
                // 设置createdat字段的取值。
                .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                // 设置updatedat字段的取值。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 11, 0))
                // 设置消息字段的取值。
                .messages(List.of(ChatMessageResponse.builder()
                        // 设置id字段的取值。
                        .id("message-1")
                        // 设置role字段的取值。
                        .role("user")
                        // 设置内容字段的取值。
                        .content("hello")
                        // 设置createdat字段的取值。
                        .createdAt(LocalDateTime.of(2026, 4, 16, 10, 1))
                        // 完成当前建造者对象的组装。
                        .build()))
                // 完成当前建造者对象的组装。
                .build();
    }
}
