// 声明当前源文件所属包。
package com.inote.config;

import com.inote.logging.RuntimeEndpointHolder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

// 将当前类注册为通用组件。
@Component
// 定义 Servlet 容器初始化器，用于兼容外部容器部署。
public class ServerEndpointInitializer implements ApplicationListener<WebServerInitializedEvent> {

    /**
     * 处理onapplicationevent相关逻辑。
     * @param event event参数。
     */
    // 声明当前方法重写父类或接口定义。
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // 更新port字段。
        RuntimeEndpointHolder.setPort(event.getWebServer().getPort());
    }
}
