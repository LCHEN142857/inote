// 声明包路径，客户端层
package com.inote.client;

// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 Spring AI 聊天模型接口
import org.springframework.ai.chat.model.ChatModel;
// 导入 Spring AI 聊天响应类
import org.springframework.ai.chat.model.ChatResponse;
// 导入 Spring AI 聊天选项接口
import org.springframework.ai.chat.prompt.ChatOptions;
// 导入 Spring AI 提示词类
import org.springframework.ai.chat.prompt.Prompt;
// 导入 OpenAI 兼容聊天模型实现
import org.springframework.ai.openai.OpenAiChatModel;
// 导入 OpenAI 兼容聊天配置选项
import org.springframework.ai.openai.OpenAiChatOptions;
// 导入 OpenAI 兼容 API 客户端
import org.springframework.ai.openai.api.OpenAiApi;
// 导入重试退避策略注解
import org.springframework.retry.annotation.Backoff;
// 导入可重试注解
import org.springframework.retry.annotation.Retryable;

/**
 * 容错聊天模型
 * 实现 ChatModel 接口，提供主模型 → 备选模型的自动故障转移机制
 * 当主模型（通义千问）调用失败时，自动切换到备选模型（Kimi）
 */
// 自动创建 Slf4j 日志对象 log
@Slf4j
// 实现 ChatModel 接口，可被 Spring AI 框架识别为聊天模型
public class FallbackChatModel implements ChatModel {

    // 备选模型名称（如 kimi-2.5）
    private final String fallbackModelName;
    // 备选模型的 API 密钥
    private final String fallbackApiKey;
    // 备选模型的 API 基础地址
    private final String fallbackBaseUrl;
    // 实际的备选 ChatModel 实例，初始化后可用于调用
    private ChatModel fallbackChatModel;

    // 构造函数，接收备选模型配置
    public FallbackChatModel(String fallbackModelName, String fallbackApiKey, String fallbackBaseUrl) {
        // 保存备选模型名称
        this.fallbackModelName = fallbackModelName;
        // 保存备选模型 API 密钥
        this.fallbackApiKey = fallbackApiKey;
        // 保存备选模型 API 地址
        this.fallbackBaseUrl = fallbackBaseUrl;
        // 初始化备选模型实例
        initFallbackModel();
    }

    // 初始化备选 ChatModel 实例
    private void initFallbackModel() {
        try {
            // 检查 API 密钥不为 null、不为空、且不是默认的占位符值
            if (fallbackApiKey != null && !fallbackApiKey.isEmpty()
                    && !fallbackApiKey.equals("your-kimi-api-key-here")) {
                // 创建 OpenAI 兼容 API 客户端，指向备选模型的 API 地址
                OpenAiApi openAiApi = new OpenAiApi(fallbackBaseUrl, fallbackApiKey);
                // 构建聊天选项
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        // 设置模型名称
                        .model(fallbackModelName)
                        // 设置生成温度（0-1），0.7 平衡创造性和准确性
                        .temperature(0.7)
                        // 设置最大生成 token 数
                        .maxTokens(2000)
                        // 构建选项对象
                        .build();

                // 创建备选聊天模型实例
                this.fallbackChatModel = new OpenAiChatModel(openAiApi, options);
                // 记录初始化成功的日志
                log.info("Fallback model initialized: {}", fallbackModelName);
            } else {
                // API 密钥未配置，备选模型不可用
                log.warn("Fallback API key not configured, fallback model will not be available");
            }
        // 捕获初始化过程中的异常
        } catch (Exception e) {
            // 记录初始化失败的错误日志
            log.error("Failed to initialize fallback model: {}", e.getMessage());
        }
    }

    // 覆盖 ChatModel 接口的 call 方法
    @Override
    // 启用自动重试机制
    @Retryable(
            // 捕获所有异常时触发重试
            retryFor = {Exception.class},
            // 最多重试 2 次
            maxAttempts = 2,
            // 重试间隔 1000 毫秒（1 秒）
            backoff = @Backoff(delay = 1000)
    )
    // 直接调用时抛出异常，因为 FallbackChatModel 不应被直接调用
    public ChatResponse call(Prompt prompt) {
        // 此方法不应被直接调用，应使用 callWithFallback 方法
        throw new UnsupportedOperationException(
            "FallbackChatModel should be used with AiConfig to wrap the primary model"
        );
    }

    /**
     * 执行带容错的模型调用
     * 先尝试主模型，主模型失败后自动切换到备选模型
     * @param primaryModel 主聊天模型（通义千问）
     * @param prompt 提示词
     * @return 聊天响应
     */
    public ChatResponse callWithFallback(ChatModel primaryModel, Prompt prompt) {
        try {
            // 记录尝试调用主模型
            log.debug("Trying primary model...");
            // 调用主模型发送请求
            ChatResponse response = primaryModel.call(prompt);
            // 记录主模型调用成功
            log.debug("Primary model succeeded");
            // 返回主模型的响应
            return response;
        // 捕获主模型调用失败的异常
        } catch (Exception e) {
            // 记录主模型失败，准备切换到备选模型
            log.warn("Primary model failed: {}, trying fallback model...", e.getMessage());

            // 检查备选模型是否已初始化
            if (fallbackChatModel != null) {
                try {
                    // 调用备选模型发送请求
                    ChatResponse fallbackResponse = fallbackChatModel.call(prompt);
                    // 记录备选模型调用成功
                    log.info("Fallback model succeeded");
                    // 返回备选模型的响应
                    return fallbackResponse;
                // 捕获备选模型也失败的异常
                } catch (Exception fallbackException) {
                    // 记录备选模型也失败
                    log.error("Fallback model also failed: {}", fallbackException.getMessage());
                    // 抛出运行时异常，两个模型都失败
                    throw new RuntimeException("Both primary and fallback models failed", fallbackException);
                }
            } else {
                // 主模型失败且没有可用的备选模型
                throw new RuntimeException("Primary model failed and no fallback available", e);
            }
        }
    }

    // 覆盖 ChatModel 接口的 getDefaultOptions 方法
    @Override
    // 获取默认聊天选项
    public ChatOptions getDefaultOptions() {
        // 如果备选模型已初始化
        if (fallbackChatModel != null) {
            // 返回备选模型的默认选项
            return fallbackChatModel.getDefaultOptions();
        }
        // 备选模型未初始化时返回 null
        return null;
    }
}
