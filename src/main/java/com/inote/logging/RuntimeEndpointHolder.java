// 声明当前源文件的包。
package com.inote.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;

// 处理当前代码结构。
public final class RuntimeEndpointHolder {

    /**
     * 描述 `resolveHostAddress` 操作。
     *
     * @return 类型为 `String host =` 的返回值。
     */
    // 执行当前语句。
    private static volatile String host = resolveHostAddress();
    // 声明当前字段。
    private static volatile int port = 0;

    /**
     * 描述 `RuntimeEndpointHolder` 操作。
     *
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    private RuntimeEndpointHolder() {
    // 结束当前代码块。
    }

    /**
     * 描述 `setPort` 操作。
     *
     * @param runtimePort 输入参数 `runtimePort`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public static void setPort(int runtimePort) {
        // 执行当前语句。
        port = runtimePort;
    // 结束当前代码块。
    }

    /**
     * 描述 `getEndpoint` 操作。
     *
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    public static String getEndpoint() {
        // 返回当前结果。
        return host + ":" + port;
    // 结束当前代码块。
    }

    /**
     * 描述 `resolveHostAddress` 操作。
     *
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private static String resolveHostAddress() {
        // 执行当前流程控制分支。
        try {
            // 返回当前结果。
            return InetAddress.getLocalHost().getHostAddress();
        // 处理当前代码结构。
        } catch (UnknownHostException ex) {
            // 返回当前结果。
            return "127.0.0.1";
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }
// 结束当前代码块。
}
