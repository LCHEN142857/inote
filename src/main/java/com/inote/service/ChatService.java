package com.inote.service;

import com.inote.client.FallbackChatModel;
import com.inote.model.dto.ChatResponse;
import com.inote.model.dto.SourceReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EmbeddingService embeddingService;

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;

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
     * @param question 用户问题
     * @return 回答和来源
     */
    public ChatResponse query(String question) {
        log.info("Processing query: {}", question);

        // 1. 向量检索相似文档
        List<Document> relevantDocs = embeddingService.searchSimilarDocuments(question, TOP_K);
        
        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found for query: {}", question);
            return ChatResponse.builder()
                    .answer("抱歉，知识库中没有找到与您问题相关的文档内容。")
                    .sources(List.of())
                    .build();
        }

        // 2. 构建上下文
        String context = buildContext(relevantDocs);
        
        // 3. 构建 Prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        Prompt prompt = systemPromptTemplate.create(Map.of("context", context));
        
        // 4. 调用大模型（带容错）
        String answer = callModelWithFallback(prompt);
        
        // 5. 提取来源信息
        List<SourceReference> sources = extractSources(relevantDocs);

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("[文档 ").append(i + 1).append("]\n");
            context.append(doc.getText());
            context.append("\n\n");
        }
        return context.toString().trim();
    }

    /**
     * 调用大模型（带容错机制）
     */
    private String callModelWithFallback(Prompt prompt) {
        try {
            log.debug("Calling primary model...");
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, prompt);
            
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            
            return "抱歉，模型未能生成有效回答。";
            
        } catch (Exception e) {
            log.error("Both primary and fallback models failed", e);
            return "抱歉，服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 提取来源信息
     */
    private List<SourceReference> extractSources(List<Document> documents) {
        Set<String> seenUrls = new HashSet<>();
        
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();
                    String fileName = metadata.getOrDefault("file_name", "未知文件").toString();
                    String url = metadata.getOrDefault("file_url", "").toString();
                    
                    return SourceReference.builder()
                            .fileName(fileName)
                            .url(url)
                            .build();
                })
                .filter(source -> {
                    // 去重
                    if (seenUrls.contains(source.getUrl())) {
                        return false;
                    }
                    seenUrls.add(source.getUrl());
                    return true;
                })
                .limit(3) // 最多返回3个来源
                .collect(Collectors.toList());
    }
}
