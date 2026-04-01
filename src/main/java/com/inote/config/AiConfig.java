package com.inote.config;

import com.inote.client.FallbackChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;

@Configuration
public class AiConfig {

    @Value("${ai.fallback.model:kimi-2.5}")
    private String fallbackModel;

    @Value("${ai.fallback.api-key:}")
    private String fallbackApiKey;

    @Value("${ai.fallback.base-url:https://api.moonshot.cn/v1}")
    private String fallbackBaseUrl;

    /**
     * 主 ChatModel - 使用 Spring AI Alibaba 自动配置的模型
     */
    @Bean
    @Primary
    public ChatModel chatModel(
            @Qualifier("dashscopeChatModel") ChatModel primaryChatModel) {
        return primaryChatModel;
    }

    /**
     * Fallback ChatModel - 备选模型
     */
    @Bean
    public ChatModel fallbackChatModel() {
        // 使用 FallbackChatModel 包装主模型，实现自动切换逻辑
        return new FallbackChatModel(fallbackModel, fallbackApiKey, fallbackBaseUrl);
    }

    /**
     * EmbeddingModel - 使用 Spring AI Alibaba 自动配置的嵌入模型
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
}
