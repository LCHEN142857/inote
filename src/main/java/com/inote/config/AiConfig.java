// 声明包路径，配置层
package com.inote.config;

// 导入容错聊天模型类
import com.inote.client.FallbackChatModel;
// 导入 Spring AI 聊天模型接口
import org.springframework.ai.chat.model.ChatModel;
// 导入 Spring AI OpenAI 聊天模型实现
import org.springframework.ai.openai.OpenAiChatModel;
// 导入配置值注入注解
import org.springframework.beans.factory.annotation.Value;
// 导入 Bean 定义注解
import org.springframework.context.annotation.Bean;
// 导入配置类注解
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
// AI 模型配置类，配置主模型和备选模型
public class AiConfig {

    // 从配置文件读取备选模型名称，默认值为 kimi-2.5
    @Value("${ai.fallback.model:kimi-2.5}")
    // 备选模型名称
    private String fallbackModel;

    // 从配置文件读取备选模型 API 密钥，默认为空
    @Value("${ai.fallback.api-key:}")
    // 备选模型 API 密钥
    private String fallbackApiKey;

    // 从配置文件读取备选模型 API 地址，默认为 Kimi（月之暗面）API
    @Value("${ai.fallback.base-url:https://api.moonshot.cn/v1}")
    // 备选模型 API 基础地址
    private String fallbackBaseUrl;

    /**
     * 主 ChatModel Bean
     * 使用 Spring AI OpenAI 自动配置的模型（实际指向 DashScope 阿里云通义千问）
     */
    // 将返回值注册为 Spring Bean
    @Bean
    // 标记为主要 Bean，当存在多个 ChatModel 类型时优先注入此 Bean
    @Primary
    // 接收自动配置的 OpenAiChatModel 实例
    public ChatModel chatModel(OpenAiChatModel openAiChatModel) {
        // 将自动配置的 OpenAI 兼容模型作为主聊天模型返回
        return openAiChatModel;
    }

    /**
     * Fallback ChatModel Bean - 备选聊天模型
     * 当主模型不可用时自动切换到此模型
     */
    // 将返回值注册为 Spring Bean
    @Bean
    // 创建备选聊天模型
    public FallbackChatModel fallbackChatModel() {
        // 使用配置的模型名称、密钥和地址初始化备选模型
        return new FallbackChatModel(fallbackModel, fallbackApiKey, fallbackBaseUrl);
    }
}
