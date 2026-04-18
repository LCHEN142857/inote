// 声明当前源文件所属包。
package com.inote.security;

// 定义未授权异常类型，表示当前请求缺少合法认证。
public class UnauthorizedException extends RuntimeException {

    /**
     * 处理unauthorizedexception相关逻辑。
     * @param message 消息参数。
     */
    public UnauthorizedException(String message) {
        // 调用 `super` 完成当前步骤。
        super(message);
    }
}
