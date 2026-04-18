// 声明当前源文件所属包。
package com.inote.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

// 定义运行时端点转换器，用于输出当前请求端点。
public class RuntimeEndpointConverter extends ClassicConverter {

    /**
     * 处理convert相关逻辑。
     * @param event event参数。
     * @return 处理后的字符串结果。
     */
    // 声明当前方法重写父类或接口定义。
    @Override
    public String convert(ILoggingEvent event) {
        // 返回 `getEndpoint` 的处理结果。
        return RuntimeEndpointHolder.getEndpoint();
    }
}
