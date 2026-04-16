// 声明当前源文件的包。
package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

// 声明当前类型。
public class RuntimeEndpointConverter extends ClassicConverter {

    /**
     * 描述 `convert` 操作。
     *
     * @param event 输入参数 `event`。
     * @return 类型为 `String` 的返回值。
     */
    // 应用当前注解。
    @Override
    // 处理当前代码结构。
    public String convert(ILoggingEvent event) {
        // 返回当前结果。
        return RuntimeEndpointHolder.getEndpoint();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
