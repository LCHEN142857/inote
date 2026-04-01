package com.inote.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.List;

/**
 * FallbackChatModel - 容错聊天模型
 * 当主模型调用失败时，自动切换到备选模型
 */
@Slf4j
public class FallbackChatModel implements ChatModel {

    private final String fallbackModelName;
    private final String fallbackApiKey;
    private final String fallbackBaseUrl;
    private ChatModel fallbackChatModel;

    public FallbackChatModel(String fallbackModelName, String fallbackApiKey, String fallbackBaseUrl) {
        this.fallbackModelName = fallbackModelName;
        this.fallbackApiKey = fallbackApiKey;
        this.fallbackBaseUrl = fallbackBaseUrl;
        initFallbackModel();
    }

    private void initFallbackModel() {
        try {
            if (fallbackApiKey != null && !fallbackApiKey.isEmpty() 
                    && !fallbackApiKey.equals("your-kimi-api-key-here")) {
                OpenAiApi openAiApi = OpenAiApi.builder()
                        .apiKey(fallbackApiKey)
                        .baseUrl(fallbackBaseUrl)
                        .build();
                
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .model(fallbackModelName)
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build();
                
                this.fallbackChatModel = new OpenAiChatModel(openAiApi, options);
                log.info("Fallback model initialized: {}", fallbackModelName);
            } else {
                log.warn("Fallback API key not configured, fallback model will not be available");
            }
        } catch (Exception e) {
            log.error("Failed to initialize fallback model: {}", e.getMessage());
        }
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 1000)
    )
    public ChatResponse call(Prompt prompt) {
        // 这里由 Spring AI 自动注入的主模型处理
        // 如果主模型失败，会抛出异常，由调用方处理 fallback
        throw new UnsupportedOperationException(
            "FallbackChatModel should be used with AiConfig to wrap the primary model"
        );
    }

    /**
     * 执行带 fallback 的调用
     * @param primaryModel 主模型
     * @param prompt 提示词
     * @return 聊天响应
     */
    public ChatResponse callWithFallback(ChatModel primaryModel, Prompt prompt) {
        try {
            log.debug("Trying primary model...");
            ChatResponse response = primaryModel.call(prompt);
            log.debug("Primary model succeeded");
            return response;
        } catch (Exception e) {
            log.warn("Primary model failed: {}, trying fallback model...", e.getMessage());
            
            if (fallbackChatModel != null) {
                try {
                    ChatResponse fallbackResponse = fallbackChatModel.call(prompt);
                    log.info("Fallback model succeeded");
                    return fallbackResponse;
                } catch (Exception fallbackException) {
                    log.error("Fallback model also failed: {}", fallbackException.getMessage());
                    throw new RuntimeException("Both primary and fallback models failed", fallbackException);
                }
            } else {
                throw new RuntimeException("Primary model failed and no fallback available", e);
            }
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        if (fallbackChatModel != null) {
            return fallbackChatModel.getDefaultOptions();
        }
        return null;
    }
}
