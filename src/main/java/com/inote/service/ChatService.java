// 声明包路径，服务层
package com.inote.service;

// 导入容错聊天模型
import com.inote.client.FallbackChatModel;
// 导入统一响应 DTO
import com.inote.model.dto.InoteResponse;
// 导入引用来源 DTO
import com.inote.model.dto.SourceReference;
// 导入 Lombok 构造函数注解
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 Spring AI 消息接口
import org.springframework.ai.chat.messages.Message;
// 导入 Spring AI 用户消息类
import org.springframework.ai.chat.messages.UserMessage;
// 导入 Spring AI 聊天模型接口
import org.springframework.ai.chat.model.ChatModel;
// 导入 Spring AI 聊天响应类
import org.springframework.ai.chat.model.ChatResponse;
// 导入 Spring AI 提示词类
import org.springframework.ai.chat.prompt.Prompt;
// 导入 Spring AI 系统提示词模板
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
// 导入 Spring AI 文档类
import org.springframework.ai.document.Document;
// 导入 Spring 服务注解
import org.springframework.stereotype.Service;

// 导入 HashSet 集合，用于去重
import java.util.HashSet;
// 导入 List 集合接口
import java.util.List;
// 导入 Map 集合接口
import java.util.Map;
// 导入 Set 集合接口
import java.util.Set;
// 导入流式收集器
import java.util.stream.Collectors;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 服务层组件
@Service
// 为所有 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
// 聊天服务类，核心业务逻辑：向量检索 + 大模型问答
public class ChatService {

    // 注入主聊天模型（通义千问，通过 DashScope 调用）
    private final ChatModel chatModel;
    // 注入容错聊天模型（Kimi 备选）
    private final FallbackChatModel fallbackChatModel;
    // 注入向量嵌入服务，用于相似文档检索
    private final EmbeddingService embeddingService;

    // 向量检索返回的最相似文档数量
    private static final int TOP_K = 1;
    // 相似度阈值（当前未使用，预留用于过滤低相似度结果）
    private static final double SIMILARITY_THRESHOLD = 0.7;

    // 系统提示词模板，{context} 是占位符，运行时会被替换为检索到的文档内容
    private static final String SYSTEM_PROMPT = """
            你是一个智能知识库助手。请基于以下提供的参考文档内容回答用户的问题。

            要求：
            1. 仅基于提供的参考文档内容回答问题，不要添加外部知识
            2. 如果参考文档中没有相关信息，请明确告知用户"根据现有文档无法回答该问题"
            3. 回答时请引用相关文档来源
            4. 保持回答简洁、准确

            参考文档内容：
            {context}
            """;

    /**
     * 处理用户问答请求
     * 核心流程：向量检索 → 构建上下文 → 调用大模型 → 返回结果
     * @param question 用户问题
     * @return 包含回答和来源的响应对象
     */
    public InoteResponse query(String question) {
        // 记录用户问题到日志
        log.info("Processing query: {}", question);

        // 1. 向量检索：将用户问题转为向量，在 Milvus 中检索最相似的文档片段
        List<Document> relevantDocs = embeddingService.searchSimilarDocuments(question, TOP_K);

        // 如果没有找到任何相似文档
        if (relevantDocs.isEmpty()) {
            // 记录警告日志
            log.warn("No relevant documents found for query: {}", question);
            // 构建无结果响应
            return InoteResponse.builder()
                    // 提示用户知识库中无相关内容
                    .answer("抱歉，知识库中没有找到与您问题相关的文档内容。")
                    // 空的来源列表
                    .sources(List.of())
                    // 构建响应对象
                    .build();
        }

        // 2. 构建上下文：将检索到的文档片段拼接成上下文文本
        String context = buildContext(relevantDocs);

        // 3. 构建 Prompt：系统消息（含上下文）+ 用户消息（问题）
        // 创建系统提示词模板
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        // 用检索到的文档内容替换模板中的 {context} 占位符，生成系统消息
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        // 组合系统消息和用户问题消息，构建完整的 Prompt
        Prompt prompt = new Prompt(List.of(systemMessage, new UserMessage(question)));

        // 4. 调用大模型生成回答（带容错机制：主模型失败自动切换备选模型）
        String answer = callModelWithFallback(prompt);

        // 5. 从检索到的文档中提取来源信息
        List<SourceReference> sources = extractSources(relevantDocs);

        // 构建最终响应对象
        return InoteResponse.builder()
                // 设置大模型生成的回答
                .answer(answer)
                // 设置引用来源列表
                .sources(sources)
                // 构建并返回响应
                .build();
    }

    /**
     * 构建上下文：将检索到的文档列表拼接为上下文字符串
     * 格式为 [文档 1]\n内容\n\n[文档 2]\n内容\n\n...
     */
    private String buildContext(List<Document> documents) {
        // 创建可变字符串用于拼接
        StringBuilder context = new StringBuilder();
        // 遍历所有文档
        for (int i = 0; i < documents.size(); i++) {
            // 获取当前文档
            Document doc = documents.get(i);
            // 添加文档编号标记
            context.append("[文档 ").append(i + 1).append("]\n");
            // 添加文档文本内容
            context.append(doc.getText());
            // 文档之间用空行分隔
            context.append("\n\n");
        }
        // 去除首尾空白后返回
        return context.toString().trim();
    }

    /**
     * 调用大模型（带容错机制）
     * 先尝试主模型（通义千问），失败后自动切换到备选模型（Kimi）
     */
    private String callModelWithFallback(Prompt prompt) {
        try {
            // 记录调用主模型的日志
            log.debug("Calling primary model...");
            // 通过容错模型调用：先尝试主模型，失败后自动切换备选模型
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, prompt);

            // 检查响应结果和输出内容是否存在
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                // 提取并返回模型生成的文本内容
                return response.getResult().getOutput().getText();
            }

            // 响应为空时返回提示信息
            return "抱歉，模型未能生成有效回答。";

        // 捕获主模型和备选模型都失败的异常
        } catch (Exception e) {
            // 记录错误日志
            log.error("Both primary and fallback models failed", e);
            // 返回友好的错误提示
            return "抱歉，服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 从检索到的文档中提取来源引用信息
     * 去重后最多返回 3 个来源
     */
    private List<SourceReference> extractSources(List<Document> documents) {
        // 用于记录已出现的 URL，实现去重
        Set<String> seenUrls = new HashSet<>();

        // 将文档列表转为流式处理
        return documents.stream()
                // 将每个文档映射为 SourceReference 对象
                .map(doc -> {
                    // 获取文档的元数据
                    Map<String, Object> metadata = doc.getMetadata();
                    // 从元数据中获取文件名，默认 "未知文件"
                    String fileName = metadata.getOrDefault("file_name", "未知文件").toString();
                    // 从元数据中获取文件 URL，默认空字符串
                    String url = metadata.getOrDefault("file_url", "").toString();

                    // 构建来源引用对象
                    return SourceReference.builder()
                            // 设置文件名
                            .fileName(fileName)
                            // 设置文件 URL
                            .url(url)
                            // 构建对象
                            .build();
                })
                // 过滤重复的来源（按 URL 去重）
                .filter(source -> {
                    // 如果该 URL 已经出现过
                    if (seenUrls.contains(source.getUrl())) {
                        // 过滤掉重复项
                        return false;
                    }
                    // 记录该 URL 为已出现
                    seenUrls.add(source.getUrl());
                    // 保留该来源
                    return true;
                })
                // 最多返回 3 个来源引用
                .limit(3)
                // 收集为 List 返回
                .collect(Collectors.toList());
    }
}
