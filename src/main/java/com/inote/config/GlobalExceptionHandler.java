package com.inote.config;

import com.inote.security.UnauthorizedException;
import com.inote.service.LoginLockException;
import com.inote.service.DocumentStorageException;
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
import java.util.Map;

// 将控制器抛出的常见异常统一转换为前端可处理的 HTTP 响应。
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验失败并按字段返回错误信息。
     * @param ex Spring 参数校验异常。
     * @return 字段名到错误消息的 400 响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 汇总每个字段的校验错误，便于前端定位表单项。
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * 处理上传文件超过配置大小的异常。
     * @param ex 上传大小超限异常。
     * @return 413 错误响应。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the configured size limit.");
    }

    /**
     * 处理业务参数不合法异常。
     * @param ex 参数异常。
     * @return 400 错误响应。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 处理资源不存在异常。
     * @param ex 实体不存在异常。
     * @return 404 错误响应。
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * 处理未认证或认证失效异常。
     * @param ex 未授权异常。
     * @return 401 错误响应。
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * 处理登录失败次数过多导致的账号锁定。
     * @param ex 登录锁定异常。
     * @return 包含锁定倒计时信息的 423 响应。
     */
    @ExceptionHandler(LoginLockException.class)
    public ResponseEntity<Map<String, Object>> handleLoginLock(LoginLockException ex) {
        // 返回锁定秒数和结束时间，支持前端展示倒计时。
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("lockSeconds", ex.getLockSeconds());
        body.put("lockedUntilEpochMillis", ex.getLockedUntilEpochMillis());
        return ResponseEntity.status(HttpStatus.LOCKED).body(body);
    }

    /**
     * 处理文档上传文件保存失败。
     * @param ex 文档存储异常。
     * @return 500 错误响应。
     */
    @ExceptionHandler(DocumentStorageException.class)
    public ResponseEntity<Map<String, String>> handleDocumentStorage(DocumentStorageException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * 兜底处理未被显式捕获的异常。
     * @param ex 未知异常。
     * @return 通用 500 错误响应。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // 记录完整堆栈，响应中只暴露稳定的通用错误文案。
        log.error("Unexpected error occurred", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error. Please try again later.");
    }

    /**
     * 构造统一错误响应体。
     * @param status HTTP 状态。
     * @param message 返回给前端的错误消息。
     * @return 统一格式的错误响应。
     */
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        // 使用 error 字段统一承载错误消息。
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
