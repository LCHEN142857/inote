// 声明包路径，配置层
package com.inote.config;

// 导入统一响应 DTO
import com.inote.model.dto.InoteResponse;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 HTTP 状态码枚举
import org.springframework.http.HttpStatus;
// 导入 Spring HTTP 响应实体
import org.springframework.http.ResponseEntity;
// 导入字段校验错误类
import org.springframework.validation.FieldError;
// 导入方法参数校验异常
import org.springframework.web.bind.MethodArgumentNotValidException;
// 导入异常处理器注解
import org.springframework.web.bind.annotation.ExceptionHandler;
// 导入全局 REST 控制器增强注解
import org.springframework.web.bind.annotation.RestControllerAdvice;
// 导入文件大小超限异常
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// 导入 HashMap 集合类
import java.util.HashMap;
// 导入 List 集合接口
import java.util.List;
// 导入 Map 集合接口
import java.util.Map;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 全局异常处理器，捕获所有 Controller 抛出的异常并返回 JSON 格式响应
@RestControllerAdvice
// 全局异常处理类，统一处理各类异常并返回友好的错误信息
public class GlobalExceptionHandler {

    // 处理请求参数校验失败的异常（如 @NotBlank 校验不通过）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 返回字段名到错误信息的映射
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            // 接收校验异常对象
            MethodArgumentNotValidException ex) {
        // 创建存储错误信息的 Map
        Map<String, String> errors = new HashMap<>();
        // 遍历所有校验错误
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // 获取校验失败的字段名
            String fieldName = ((FieldError) error).getField();
            // 获取校验失败的错误提示信息
            String errorMessage = error.getDefaultMessage();
            // 将字段名和错误信息放入 Map
            errors.put(fieldName, errorMessage);
        });
        // 返回 400 Bad Request 响应，附带所有校验错误详情
        return ResponseEntity.badRequest().body(errors);
    }

    // 处理文件上传大小超过限制的异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    // 返回错误信息
    public ResponseEntity<Map<String, String>> handleMaxSizeException(
            // 接收超限异常对象
            MaxUploadSizeExceededException ex) {
        // 创建存储错误信息的 Map
        Map<String, String> error = new HashMap<>();
        // 设置错误提示信息
        error.put("error", "文件大小超过限制");
        // 返回 413 Payload Too Large 响应
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    // 处理非法参数异常（如不支持的文件格式）
    @ExceptionHandler(IllegalArgumentException.class)
    // 返回错误信息
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            // 接收非法参数异常对象
            IllegalArgumentException ex) {
        // 创建存储错误信息的 Map
        Map<String, String> error = new HashMap<>();
        // 将异常消息作为错误提示
        error.put("error", ex.getMessage());
        // 返回 400 Bad Request 响应
        return ResponseEntity.badRequest().body(error);
    }

    // 兜底处理所有未被前面处理器捕获的异常
    @ExceptionHandler(Exception.class)
    // 返回统一响应格式
    public ResponseEntity<InoteResponse> handleGenericException(Exception ex) {
        // 记录完整的异常堆栈到日志
        log.error("Unexpected error occurred", ex);
        // 构建错误响应
        InoteResponse response = InoteResponse.builder()
                // 设置友好的错误提示给用户
                .answer("抱歉，系统发生错误，请稍后重试。")
                // 设置空的来源列表
                .sources(List.of())
                // 构建响应对象
                .build();
        // 返回 500 Internal Server Error 响应
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
