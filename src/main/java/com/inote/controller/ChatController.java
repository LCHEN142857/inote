// 声明包路径，控制器层
package com.inote.controller;

// 导入聊天请求 DTO
import com.inote.model.dto.ChatRequest;
// 导入统一响应 DTO
import com.inote.model.dto.InoteResponse;
// 导入聊天服务类
import com.inote.service.ChatService;
// 导入 JSR-380 参数校验注解
import jakarta.validation.Valid;
// 导入 Lombok 注解，为 final 字段生成构造函数
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解，自动创建 log 对象
import lombok.extern.slf4j.Slf4j;
// 导入 Spring HTTP 响应实体
import org.springframework.http.ResponseEntity;
// 导入 POST 请求映射注解
import org.springframework.web.bind.annotation.PostMapping;
// 导入请求体绑定注解
import org.springframework.web.bind.annotation.RequestBody;
// 导入请求路径映射注解
import org.springframework.web.bind.annotation.RequestMapping;
// 导入 REST 控制器注解
import org.springframework.web.bind.annotation.RestController;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 REST 控制器，所有方法返回值自动序列化为 JSON
@RestController
// 设置该控制器的基础请求路径为 /api/v1/chat
@RequestMapping("/api/v1/chat")
// 为所有 final 字段生成构造函数，实现构造器注入
@RequiredArgsConstructor
// 聊天控制器，处理知识库问答相关的 HTTP 请求
public class ChatController {

    // 注入聊天服务，处理问答业务逻辑
    private final ChatService chatService;

    /**
     * 知识库问答接口
     * @param request 问答请求，包含用户问题
     * @return 回答内容和引用来源
     */
    // 映射 POST /api/v1/chat/query 请求
    @PostMapping("/query")
    // @Valid 触发参数校验，@RequestBody 将 JSON 请求体绑定到 ChatRequest
    public ResponseEntity<InoteResponse> query(@Valid @RequestBody ChatRequest request) {
        // 记录收到的用户问题
        log.info("Received chat query: {}", request.getQuestion());

        // 调用聊天服务处理用户问题，获取回答
        InoteResponse response = chatService.query(request.getQuestion());
        // 返回 200 OK 响应，携带回答结果
        return ResponseEntity.ok(response);
    }
}
