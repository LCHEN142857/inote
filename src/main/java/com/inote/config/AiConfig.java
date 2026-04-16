// 声明当前源文件的包。
package com.inote.config;

import com.inote.client.FallbackChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// 应用当前注解。
@Configuration
// 应用当前注解。
@EnableConfigurationProperties(RagProperties.class)
// 声明当前类型。
public class AiConfig {

    // 应用当前注解。
    @Value("${ai.fallback.model:kimi-2.5}")
    // 声明当前字段。
    private String fallbackModel;

    // 应用当前注解。
    @Value("${ai.fallback.api-key:}")
    // 声明当前字段。
    private String fallbackApiKey;

    // 应用当前注解。
    @Value("${ai.fallback.base-url:https://api.moonshot.cn/v1}")
    // 声明当前字段。
    private String fallbackBaseUrl;

    /**
     * 描述 `chatModel` 操作。
     *
     * @param openAiChatModel 输入参数 `openAiChatModel`。
     * @return 类型为 `ChatModel` 的返回值。
     */
    // 应用当前注解。
    @Bean
    // 应用当前注解。
    @Primary
    // 处理当前代码结构。
    public ChatModel chatModel(OpenAiChatModel openAiChatModel) {
        // 返回当前结果。
        return openAiChatModel;
    // 结束当前代码块。
    }

    /**
     * 描述 `fallbackChatModel` 操作。
     *
     * @return 类型为 `FallbackChatModel` 的返回值。
     */
    // 应用当前注解。
    @Bean
    // 处理当前代码结构。
    public FallbackChatModel fallbackChatModel() {
        // 返回当前结果。
        return new FallbackChatModel(fallbackModel, fallbackApiKey, fallbackBaseUrl);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
