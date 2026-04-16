package com.inote.config;

import com.inote.logging.RuntimeEndpointHolder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServerEndpointInitializer implements ApplicationListener<WebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        RuntimeEndpointHolder.setPort(event.getWebServer().getPort());
    }
}
