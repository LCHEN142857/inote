// 声明当前源文件的包。
package com.inote.config;

import com.inote.logging.RuntimeEndpointHolder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

// 应用当前注解。
@Component
// 声明当前类型。
public class ServerEndpointInitializer implements ApplicationListener<WebServerInitializedEvent> {

    /**
     * 描述 `onApplicationEvent` 操作。
     *
     * @param event 输入参数 `event`。
     * @return 无返回值。
     */
    // 应用当前注解。
    @Override
    // 处理当前代码结构。
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // 执行当前语句。
        RuntimeEndpointHolder.setPort(event.getWebServer().getPort());
    // 结束当前代码块。
    }
// 结束当前代码块。
}
