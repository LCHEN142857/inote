// 声明当前源文件所属包。
package com.inote.config;

import com.inote.client.FallbackChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// 声明当前类提供 Spring 配置。
@Configuration
// 应用 `EnableConfigurationProperties` 注解声明当前行为。
@EnableConfigurationProperties({RagProperties.class, ChatModelProperties.class})
// 定义 AI 相关配置，负责组装聊天模型和兜底模型。
public class AiConfig {

    // 从配置文件中注入当前字段的取值。
    @Value("${ai.fallback.model:kimi-2.5}")
    // 声明兜底模型变量，供后续流程使用。
    private String fallbackModel;

    // 从配置文件中注入当前字段的取值。
    @Value("${ai.fallback.api-key:}")
    // 声明兜底接口key变量，供后续流程使用。
    private String fallbackApiKey;

    // 从配置文件中注入当前字段的取值。
    @Value("${ai.fallback.base-url:https://api.moonshot.cn/v1}")
    // 声明兜底baseurl变量，供后续流程使用。
    private String fallbackBaseUrl;

    /**
     * 处理问答模型相关逻辑。
     * @param openAiChatModel openai问答模型参数。
     * @return 问答模型结果。
     */
    // 声明当前方法向容器提供 Bean。
    @Bean
    // 应用 `Primary` 注解声明当前行为。
    @Primary
    public ChatModel chatModel(OpenAiChatModel openAiChatModel) {
        // 返回openai问答模型。
        return openAiChatModel;
    }

    /**
     * 处理兜底问答模型相关逻辑。
     * @return 兜底问答模型结果。
     */
    // 声明当前方法向容器提供 Bean。
    @Bean
    public FallbackChatModel fallbackChatModel() {
        // 返回 `FallbackChatModel` 的处理结果。
        return new FallbackChatModel(fallbackModel, fallbackApiKey, fallbackBaseUrl);
    }
}
