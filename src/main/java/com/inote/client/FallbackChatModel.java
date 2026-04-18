// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 定义兜底模型适配器，负责主模型失败后的降级调用。
public class FallbackChatModel implements ChatModel {

    // 声明兜底模型name变量，供后续流程使用。
    private final String fallbackModelName;
    // 声明兜底接口key变量，供后续流程使用。
    private final String fallbackApiKey;
    // 声明兜底baseurl变量，供后续流程使用。
    private final String fallbackBaseUrl;
    // 声明兜底问答模型变量，供后续流程使用。
    private ChatModel fallbackChatModel;

    /**
     * 处理兜底问答模型相关逻辑。
     * @param fallbackModelName 兜底模型name参数。
     * @param fallbackApiKey 兜底接口key参数。
     * @param fallbackBaseUrl 兜底baseurl参数。
     */
    public FallbackChatModel(String fallbackModelName, String fallbackApiKey, String fallbackBaseUrl) {
        // 计算并保存this.fallback模型name结果。
        this.fallbackModelName = fallbackModelName;
        // 计算并保存this.fallback接口key结果。
        this.fallbackApiKey = fallbackApiKey;
        // 计算并保存this.fallbackbaseurl结果。
        this.fallbackBaseUrl = fallbackBaseUrl;
        // 调用 `initFallbackModel` 完成当前步骤。
        initFallbackModel();
    }

    /**
     * 处理init兜底模型相关逻辑。
     */
    private void initFallbackModel() {
        // 进入异常保护块执行关键逻辑。
        try {
            // 根据条件判断当前分支是否执行。
            if (fallbackApiKey != null && !fallbackApiKey.isEmpty()
                    && !fallbackApiKey.equals("your-kimi-api-key-here")) {
                // 创建openai接口对象。
                OpenAiApi openAiApi = new OpenAiApi(fallbackBaseUrl, fallbackApiKey);
                // 围绕openai问答补充当前业务语句。
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        // 设置模型字段的取值。
                        .model(fallbackModelName)
                        // 设置temperature字段的取值。
                        .temperature(0.7)
                        // 设置maxtokens字段的取值。
                        .maxTokens(2000)
                        // 完成当前建造者对象的组装。
                        .build();

                // 创建this.fallback问答模型对象。
                this.fallbackChatModel = new OpenAiChatModel(openAiApi, options);
                // 记录当前流程的运行日志。
                log.info("Fallback model initialized: {}", fallbackModelName);
            } else {
                // 记录当前流程的运行日志。
                log.warn("Fallback API key not configured, fallback model will not be available");
            }
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.error("Failed to initialize fallback model: {}", e.getMessage());
        }
    }

    // 声明当前方法重写父类或接口定义。
    @Override
    // 应用 `Retryable` 注解声明当前行为。
    @Retryable(
            // 定义当前类型。
            retryFor = {Exception.class},
            // 围绕maxattempts补充当前业务语句。
            maxAttempts = 2,
            // 围绕backoffbackoffdelay补充当前业务语句。
            backoff = @Backoff(delay = 1000)
    // 继续补全当前链式调用或多行表达式。
    )
    /**
     * 处理call相关逻辑。
     * @param prompt 提示词参数。
     * @return 模型返回的聊天响应。
     */
    public ChatResponse call(Prompt prompt) {
        // 抛出 `UnsupportedOperationException` 异常中断当前流程。
        throw new UnsupportedOperationException(
            "FallbackChatModel should be used with AiConfig to wrap the primary model"
        );
    }

    /**
     * 处理callwith兜底相关逻辑。
     * @param primaryModel primary模型参数。
     * @param prompt 提示词参数。
     * @return 模型返回的聊天响应。
     */
    public ChatResponse callWithFallback(ChatModel primaryModel, Prompt prompt) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 记录当前流程的运行日志。
            log.debug("Trying primary model...");
            // 调用模型生成响应结果。
            ChatResponse response = primaryModel.call(prompt);
            // 记录当前流程的运行日志。
            log.debug("Primary model succeeded");
            // 返回响应。
            return response;
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("Primary model failed: {}, trying fallback model...", e.getMessage());

            // 根据条件判断当前分支是否执行。
            if (fallbackChatModel != null) {
                // 进入异常保护块执行关键逻辑。
                try {
                    // 调用模型生成兜底响应结果。
                    ChatResponse fallbackResponse = fallbackChatModel.call(prompt);
                    // 记录当前流程的运行日志。
                    log.info("Fallback model succeeded");
                    // 返回兜底响应。
                    return fallbackResponse;
                } catch (Exception fallbackException) {
                    // 记录当前流程的运行日志。
                    log.error("Fallback model also failed: {}", fallbackException.getMessage());
                    // 抛出 `RuntimeException` 异常中断当前流程。
                    throw new RuntimeException("Both primary and fallback models failed", fallbackException);
                }
            } else {
                // 抛出 `RuntimeException` 异常中断当前流程。
                throw new RuntimeException("Primary model failed and no fallback available", e);
            }
        }
    }

    /**
     * 处理getdefaultoptions相关逻辑。
     * @return 问答options结果。
     */
    // 声明当前方法重写父类或接口定义。
    @Override
    public ChatOptions getDefaultOptions() {
        // 根据条件判断当前分支是否执行。
        if (fallbackChatModel != null) {
            // 返回 `getDefaultOptions` 的处理结果。
            return fallbackChatModel.getDefaultOptions();
        }
        // 返回空值，表示当前没有可用结果。
        return null;
    }
}
