// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 应用当前注解。
@RestControllerAdvice
// 声明当前类型。
public class GlobalExceptionHandler {

    /**
     * 描述 `handleValidationExceptions` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 执行当前语句。
        Map<String, String> errors = new HashMap<>();
        // 处理当前代码结构。
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // 执行当前语句。
            String fieldName = ((FieldError) error).getField();
            // 执行当前语句。
            String errorMessage = error.getDefaultMessage();
            // 执行当前语句。
            errors.put(fieldName, errorMessage);
        // 执行当前语句。
        });
        // 返回当前结果。
        return ResponseEntity.badRequest().body(errors);
    // 结束当前代码块。
    }

    /**
     * 描述 `handleMaxSizeException` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        // 执行当前语句。
        Map<String, String> error = new HashMap<>();
        // 执行当前语句。
        error.put("error", "Uploaded file exceeds the configured size limit.");
        // 返回当前结果。
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    // 结束当前代码块。
    }

    /**
     * 描述 `handleIllegalArgument` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(IllegalArgumentException.class)
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        // 执行当前语句。
        Map<String, String> error = new HashMap<>();
        // 执行当前语句。
        error.put("error", ex.getMessage());
        // 返回当前结果。
        return ResponseEntity.badRequest().body(error);
    // 结束当前代码块。
    }

    /**
     * 描述 `handleEntityNotFound` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(EntityNotFoundException.class)
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        // 执行当前语句。
        Map<String, String> error = new HashMap<>();
        // 执行当前语句。
        error.put("error", ex.getMessage());
        // 返回当前结果。
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    // 结束当前代码块。
    }

    /**
     * 描述 `handleUnauthorized` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(UnauthorizedException.class)
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        // 执行当前语句。
        Map<String, String> error = new HashMap<>();
        // 执行当前语句。
        error.put("error", ex.getMessage());
        // 返回当前结果。
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    // 结束当前代码块。
    }

    /**
     * 描述 `handleGenericException` 操作。
     *
     * @param ex 输入参数 `ex`。
     * @return 类型为 `ResponseEntity<InoteResponse>` 的返回值。
     */
    // 应用当前注解。
    @ExceptionHandler(Exception.class)
    // 处理当前代码结构。
    public ResponseEntity<InoteResponse> handleGenericException(Exception ex) {
        // 执行当前语句。
        log.error("Unexpected error occurred", ex);
        // 处理当前代码结构。
        InoteResponse response = InoteResponse.builder()
                // 处理当前代码结构。
                .answer("Unexpected server error. Please try again later.")
                // 处理当前代码结构。
                .sources(List.of())
                // 执行当前语句。
                .build();
        // 返回当前结果。
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
