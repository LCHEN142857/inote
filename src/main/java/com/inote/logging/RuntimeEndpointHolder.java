// 声明当前源文件所属包。
package com.inote.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;

// 定义运行时端点上下文，用于在线程内保存访问端点。
public final class RuntimeEndpointHolder {

    // 计算并保存host结果。
    private static volatile String host = resolveHostAddress();
    // 计算并保存port结果。
    private static volatile int port = 0;

    /**
     * 处理运行时端点holder相关逻辑。
     */
    private RuntimeEndpointHolder() {
    }

    /**
     * 处理setport相关逻辑。
     * @param runtimePort 运行时port参数。
     */
    public static void setPort(int runtimePort) {
        // 计算并保存port结果。
        port = runtimePort;
    }

    /**
     * 处理get端点相关逻辑。
     * @return 处理后的字符串结果。
     */
    public static String getEndpoint() {
        // 返回host+":"+port。
        return host + ":" + port;
    }

    /**
     * 处理resolvehostaddress相关逻辑。
     * @return 处理后的字符串结果。
     */
    private static String resolveHostAddress() {
        // 进入异常保护块执行关键逻辑。
        try {
            // 返回 `getLocalHost` 的处理结果。
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            // 返回1"。
            return "127.0.0.1";
        }
    }
}
