package com.inote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.chat")
public class ChatModelProperties {

    private String defaultModel;

    private List<String> availableModels = new ArrayList<>();
}
