package com.inote.service;

// 表示文件落盘或文件系统访问失败的业务异常。
public class DocumentStorageException extends RuntimeException {

    /**
     * 说明文档存储失败的原因。
     * @param message 异常消息，用于向上层说明存储失败的业务背景。
     * @param cause 底层 I/O 异常。
     */
    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
