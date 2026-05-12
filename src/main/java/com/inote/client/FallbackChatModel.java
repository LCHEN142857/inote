package com.inote.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
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
                OpenAiApi openAiApi = new OpenAiApi(fallbackBaseUrl, fallbackApiKey);
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
        throw new UnsupportedOperationException(
                "FallbackChatModel should be used with AiConfig to wrap the primary model"
        );
    }

    public ChatResponse callWithFallback(ChatModel primaryModel, Prompt prompt) {
        try {
            log.debug("Trying primary model [{}]...", effectiveModel(prompt));
            ChatResponse response = primaryModel.call(prompt);
            log.debug("Primary model succeeded");
            return response;
        } catch (Exception e) {
            log.warn("Primary model failed: {}, trying fallback model...", e.getMessage());

            if (fallbackChatModel != null) {
                try {
                    ChatResponse fallbackResponse = fallbackChatModel.call(withoutModel(prompt));
                    log.info("Fallback model succeeded");
                    return fallbackResponse;
                } catch (Exception fallbackException) {
                    log.error("Fallback model also failed: {}", fallbackException.getMessage());
                    throw new RuntimeException("Both primary and fallback models failed", fallbackException);
                }
            }

            throw new RuntimeException("Primary model failed and no fallback available", e);
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        if (fallbackChatModel != null) {
            return fallbackChatModel.getDefaultOptions();
        }
        return null;
    }

    private Prompt withoutModel(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        ChatOptions options = prompt.getOptions();
        if (options instanceof OpenAiChatOptions openAiChatOptions) {
            OpenAiChatOptions fallbackOptions = OpenAiChatOptions.fromOptions(openAiChatOptions);
            fallbackOptions.setModel(null);
            return new Prompt(messages, fallbackOptions);
        }
        return new Prompt(messages);
    }

    private String effectiveModel(Prompt prompt) {
        ChatOptions options = prompt.getOptions();
        if (options == null || options.getModel() == null || options.getModel().isBlank()) {
            return "default";
        }
        return options.getModel();
    }
}
