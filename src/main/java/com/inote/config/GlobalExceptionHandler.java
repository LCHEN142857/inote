// 声明当前源文件所属包。
package com.inote.config;

import com.inote.model.dto.InoteResponse;
import com.inote.security.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 启用当前类的日志记录能力。
@Slf4j
// 声明当前类提供全局 REST 异常处理能力。
@RestControllerAdvice
// 定义全局异常处理器，负责统一封装接口错误响应。
public class GlobalExceptionHandler {

    /**
     * 处理handlevalidationexceptions相关逻辑。
     * @param ex ex参数。
     * @return string>>结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 创建errors对象。
        Map<String, String> errors = new HashMap<>();
        // 围绕exgetbinding补充当前业务语句。
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // 计算并保存fieldname结果。
            String fieldName = ((FieldError) error).getField();
            // 计算并保存错误信息消息结果。
            String errorMessage = error.getDefaultMessage();
            // 写入当前映射中的键值对。
            errors.put(fieldName, errorMessage);
        });
        // 返回参数错误响应。
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * 处理handlemaxsizeexception相关逻辑。
     * @param ex ex参数。
     * @return string>>结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        // 创建错误信息对象。
        Map<String, String> error = new HashMap<>();
        // 写入当前映射中的键值对。
        error.put("error", "Uploaded file exceeds the configured size limit.");
        // 按指定状态码返回响应。
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * 处理handleillegalargument相关逻辑。
     * @param ex ex参数。
     * @return string>>结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        // 创建错误信息对象。
        Map<String, String> error = new HashMap<>();
        // 写入当前映射中的键值对。
        error.put("error", ex.getMessage());
        // 返回参数错误响应。
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 处理handleentitynotfound相关逻辑。
     * @param ex ex参数。
     * @return string>>结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        // 创建错误信息对象。
        Map<String, String> error = new HashMap<>();
        // 写入当前映射中的键值对。
        error.put("error", ex.getMessage());
        // 按指定状态码返回响应。
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理handleunauthorized相关逻辑。
     * @param ex ex参数。
     * @return string>>结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        // 创建错误信息对象。
        Map<String, String> error = new HashMap<>();
        // 写入当前映射中的键值对。
        error.put("error", ex.getMessage());
        // 按指定状态码返回响应。
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * 处理handlegenericexception相关逻辑。
     * @param ex ex参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理指定异常类型。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InoteResponse> handleGenericException(Exception ex) {
        // 记录当前流程的运行日志。
        log.error("Unexpected error occurred", ex);
        // 围绕inote响应响应补充当前业务语句。
        InoteResponse response = InoteResponse.builder()
                // 设置回答字段的取值。
                .answer("Unexpected server error. Please try again later.")
                // 设置来源字段的取值。
                .sources(List.of())
                // 完成当前建造者对象的组装。
                .build();
        // 按指定状态码返回响应。
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
