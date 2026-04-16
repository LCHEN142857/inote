// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 声明当前类型。
public class FallbackChatModel implements ChatModel {

    // 声明当前字段。
    private final String fallbackModelName;
    // 声明当前字段。
    private final String fallbackApiKey;
    // 声明当前字段。
    private final String fallbackBaseUrl;
    // 声明当前字段。
    private ChatModel fallbackChatModel;

    /**
     * 描述 `FallbackChatModel` 操作。
     *
     * @param fallbackModelName 输入参数 `fallbackModelName`。
     * @param fallbackApiKey 输入参数 `fallbackApiKey`。
     * @param fallbackBaseUrl 输入参数 `fallbackBaseUrl`。
     * @return 构造完成的实例状态。
     */
    // 处理当前代码结构。
    public FallbackChatModel(String fallbackModelName, String fallbackApiKey, String fallbackBaseUrl) {
        // 执行当前语句。
        this.fallbackModelName = fallbackModelName;
        // 执行当前语句。
        this.fallbackApiKey = fallbackApiKey;
        // 执行当前语句。
        this.fallbackBaseUrl = fallbackBaseUrl;
        // 执行当前语句。
        initFallbackModel();
    // 结束当前代码块。
    }

    /**
     * 描述 `initFallbackModel` 操作。
     *
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void initFallbackModel() {
        // 执行当前流程控制分支。
        try {
            // 执行当前流程控制分支。
            if (fallbackApiKey != null && !fallbackApiKey.isEmpty()
                    // 处理当前代码结构。
                    && !fallbackApiKey.equals("your-kimi-api-key-here")) {
                // 执行当前语句。
                OpenAiApi openAiApi = new OpenAiApi(fallbackBaseUrl, fallbackApiKey);
                // 处理当前代码结构。
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        // 处理当前代码结构。
                        .model(fallbackModelName)
                        // 处理当前代码结构。
                        .temperature(0.7)
                        // 处理当前代码结构。
                        .maxTokens(2000)
                        // 执行当前语句。
                        .build();

                // 执行当前语句。
                this.fallbackChatModel = new OpenAiChatModel(openAiApi, options);
                // 执行当前语句。
                log.info("Fallback model initialized: {}", fallbackModelName);
            // 处理当前代码结构。
            } else {
                // 执行当前语句。
                log.warn("Fallback API key not configured, fallback model will not be available");
            // 结束当前代码块。
            }
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.error("Failed to initialize fallback model: {}", e.getMessage());
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `call` 操作。
     *
     * @param prompt 输入参数 `prompt`。
     * @return 类型为 `ChatResponse` 的返回值。
     */
    // 应用当前注解。
    @Override
    // 应用当前注解。
    @Retryable(
            // 处理当前代码结构。
            retryFor = {Exception.class},
            // 处理当前代码结构。
            maxAttempts = 2,
            // 处理当前代码结构。
            backoff = @Backoff(delay = 1000)
    // 处理当前代码结构。
    )
    // 处理当前代码结构。
    public ChatResponse call(Prompt prompt) {
        // 抛出当前异常。
        throw new UnsupportedOperationException(
            // 处理当前代码结构。
            "FallbackChatModel should be used with AiConfig to wrap the primary model"
        // 执行当前语句。
        );
    // 结束当前代码块。
    }

    /**
     * 描述 `callWithFallback` 操作。
     *
     * @param primaryModel 输入参数 `primaryModel`。
     * @param prompt 输入参数 `prompt`。
     * @return 类型为 `ChatResponse` 的返回值。
     */
    // 处理当前代码结构。
    public ChatResponse callWithFallback(ChatModel primaryModel, Prompt prompt) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            log.debug("Trying primary model...");
            // 执行当前语句。
            ChatResponse response = primaryModel.call(prompt);
            // 执行当前语句。
            log.debug("Primary model succeeded");
            // 返回当前结果。
            return response;
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("Primary model failed: {}, trying fallback model...", e.getMessage());

            // 执行当前流程控制分支。
            if (fallbackChatModel != null) {
                // 执行当前流程控制分支。
                try {
                    // 执行当前语句。
                    ChatResponse fallbackResponse = fallbackChatModel.call(prompt);
                    // 执行当前语句。
                    log.info("Fallback model succeeded");
                    // 返回当前结果。
                    return fallbackResponse;
                // 处理当前代码结构。
                } catch (Exception fallbackException) {
                    // 执行当前语句。
                    log.error("Fallback model also failed: {}", fallbackException.getMessage());
                    // 抛出当前异常。
                    throw new RuntimeException("Both primary and fallback models failed", fallbackException);
                // 结束当前代码块。
                }
            // 处理当前代码结构。
            } else {
                // 抛出当前异常。
                throw new RuntimeException("Primary model failed and no fallback available", e);
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `getDefaultOptions` 操作。
     *
     * @return 类型为 `ChatOptions` 的返回值。
     */
    // 应用当前注解。
    @Override
    // 处理当前代码结构。
    public ChatOptions getDefaultOptions() {
        // 执行当前流程控制分支。
        if (fallbackChatModel != null) {
            // 返回当前结果。
            return fallbackChatModel.getDefaultOptions();
        // 结束当前代码块。
        }
        // 返回当前结果。
        return null;
    // 结束当前代码块。
    }
// 结束当前代码块。
}
