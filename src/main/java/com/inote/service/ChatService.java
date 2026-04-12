// 声明包路径，服务层
package com.inote.service;

import com.inote.client.FallbackChatModel;
import com.inote.model.dto.InoteResponse;
import com.inote.model.dto.SourceReference;
import com.inote.service.retrieval.RetrievalPipelineService;
import com.inote.service.retrieval.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;
    private final FallbackChatModel fallbackChatModel;
    private final RetrievalPipelineService retrievalPipelineService;

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

    public InoteResponse query(String question) {
        log.info("Processing query: {}", question);

        RetrievalResult result = retrievalPipelineService.retrieve(question);
        List<Document> relevantDocs = result.documents();

        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found for query: {}", question);
            return InoteResponse.builder()
                    .answer("抱歉，知识库中没有找到与您问题相关的文档内容。")
                    .sources(List.of())
                    .build();
        }

        String context = buildContext(relevantDocs);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        Prompt prompt = new Prompt(List.of(systemMessage, new UserMessage(question)));

        String answer = callModelWithFallback(prompt);
        List<SourceReference> sources = extractSources(relevantDocs);

        return InoteResponse.builder()
                .answer(answer)
                .sources(sources)
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
