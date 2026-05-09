// 声明当前源文件所属包。
package com.inote.service;

import com.inote.client.FallbackChatModel;
import com.inote.model.dto.ChatRequest;
import com.inote.model.dto.InoteResponse;
import com.inote.model.dto.SourceReference;
import com.inote.model.entity.ChatMessage;
import com.inote.model.entity.ChatSession;
import com.inote.repository.ChatMessageRepository;
import com.inote.service.retrieval.RetrievalPipelineService;
import com.inote.service.retrieval.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义问答服务，负责检索增强问答和聊天消息落库逻辑。
public class ChatService {

    // 写入当前请求体或提示词模板内容。
    private static final String SYSTEM_PROMPT = """
            You are an enterprise knowledge-base assistant.

            Rules:
            1. Answer only from the provided reference documents.
            2. If the documents do not contain the answer, say that the current documents do not provide enough information.
            3. When chat history exists, use it to understand follow-up questions.
            4. Keep the answer concise and accurate, and cite sources when possible.

            Reference documents:
            {context}
            """;

    // 计算并保存max历史消息消息结果。
    private static final int MAX_HISTORY_MESSAGES = 20;

    // 声明问答模型变量，供后续流程使用。
    private final ChatModel chatModel;
    // 声明兜底问答模型变量，供后续流程使用。
    private final FallbackChatModel fallbackChatModel;
    // 声明检索pipelineservice变量，供后续流程使用。
    private final RetrievalPipelineService retrievalPipelineService;
    // 声明问答会话service变量，供后续流程使用。
    private final ChatSessionService chatSessionService;
    // 声明问答消息repository变量，供后续流程使用。
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 执行检索增强问答，并在需要时将问答内容保存到会话。
     * @param request 请求参数。
     * @return inote响应结果。
     */
    // 声明当前方法在事务边界内执行。
    @Transactional
    public InoteResponse query(ChatRequest request) {
        // 清理并规范化问题内容。
        String question = request.getQuestion().trim();
        // 计算并保存会话id结果。
        log.info("Processing query, sessionId={}, question={}", request.getSessionId(), question);

        // 计算并保存会话结果。
        ChatSession session = resolveSession(request.getSessionId());
        // 围绕问答消息历史消息补充当前业务语句。
        List<ChatMessage> history = session == null
                ? List.of()
                : chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        // 执行检索流程并获取候选文档。
        RetrievalResult result = retrievalPipelineService.retrieve(question);
        // 计算并保存relevantdocs结果。
        List<Document> relevantDocs = result.documents();
        // 计算并保存来源结果。
        List<SourceReference> sources = extractSources(relevantDocs);

        // 声明回答变量，供后续流程使用。
        String answer;
        // 根据条件判断当前分支是否执行。
        if (relevantDocs.isEmpty()) {
            // 记录当前流程的运行日志。
            log.warn("No relevant documents found for query: {}", question);
            // 计算并保存回答结果。
            answer = "当前文档信息不足，无法回答这个问题。";
        } else {
            // 计算并保存上下文结果。
            String context = buildContext(relevantDocs);
            // 计算并保存提示词结果。
            Prompt prompt = buildPrompt(question, context, history);
            // 计算并保存回答结果。
            answer = callModelWithFallback(prompt);
        }

        // 根据条件判断当前分支是否执行。
        if (session != null) {
            // 调用 `persistConversation` 完成当前步骤。
            persistConversation(session, question, answer, history.isEmpty());
        }

        // 返回组装完成的结果对象。
        return InoteResponse.builder()
                // 设置会话id字段的取值。
                .sessionId(session == null ? null : session.getId())
                // 设置回答字段的取值。
                .answer(answer)
                // 设置来源字段的取值。
                .sources(sources)
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 组合系统提示词、历史消息和用户问题，构建模型提示词。
     * @param question 问题参数。
     * @param context 上下文参数。
     * @param history 历史消息参数。
     * @return 组装完成的模型提示词。
     */
    private Prompt buildPrompt(String question, String context, List<ChatMessage> history) {
        // 创建system提示词template对象。
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        // 构造system消息固定映射。
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));

        // 创建消息对象。
        List<Message> messages = new ArrayList<>();
        // 向当前集合中追加元素。
        messages.add(systemMessage);
        // 调用 `addAll` 完成当前步骤。
        messages.addAll(toPromptMessages(history));
        // 向当前集合中追加元素。
        messages.add(new UserMessage(question));
        // 返回 `Prompt` 的处理结果。
        return new Prompt(messages);
    }

    /**
     * 将历史聊天消息转换为模型可识别的消息列表。
     * @param history 历史消息参数。
     * @return 列表形式的处理结果。
     */
    private List<Message> toPromptMessages(List<ChatMessage> history) {
        // 计算并保存fromindex结果。
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        // 返回 `subList` 的处理结果。
        return history.subList(fromIndex, history.size()).stream()
                // 设置map字段的取值。
                .map(message -> "assistant".equalsIgnoreCase(message.getRole())
                        ? new AssistantMessage(message.getContent())
                        : new UserMessage(message.getContent()))
                // 定义当前类型。
                .map(Message.class::cast)
                // 设置tolist字段的取值。
                .toList();
    }

    /**
     * 将命中文档拼接为模型回答所需的参考上下文。
     * @param documents 文档参数。
     * @return 处理后的字符串结果。
     */
    private String buildContext(List<Document> documents) {
        // 创建上下文对象。
        StringBuilder context = new StringBuilder();
        // 遍历当前集合或区间中的元素。
        for (int i = 0; i < documents.size(); i++) {
            // 计算并保存doc结果。
            Document doc = documents.get(i);
            // 调用 `append` 完成当前步骤。
            context.append("[Document ").append(i + 1).append("]\n");
            // 调用 `append` 完成当前步骤。
            context.append(doc.getText());
            // 调用 `append` 完成当前步骤。
            context.append("\n\n");
        }
        // 返回 `toString` 的处理结果。
        return context.toString().trim();
    }

    /**
     * 优先调用主模型，失败时降级到兜底模型生成答案。
     * @param prompt 提示词参数。
     * @return 处理后的字符串结果。
     */
    private String callModelWithFallback(Prompt prompt) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 调用模型生成响应结果。
            ChatResponse response = fallbackChatModel.callWithFallback(chatModel, prompt);
            // 根据条件判断当前分支是否执行。
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                // 返回 `getResult` 的处理结果。
                return response.getResult().getOutput().getText();
            }
            // 返回"。
            return "The model did not generate a valid answer.";
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.error("Both primary and fallback models failed", e);
            // 返回"。
            return "The service is temporarily unavailable. Please try again later.";
        }
    }

    /**
     * 从命中文档元数据中提取去重后的来源信息。
     * @param documents 文档参数。
     * @return 列表形式的处理结果。
     */
    private List<SourceReference> extractSources(List<Document> documents) {
        // 创建seenurls对象。
        Set<String> seenUrls = new HashSet<>();
        // 返回 `stream` 的处理结果。
        return documents.stream()
                // 设置map字段的取值。
                .map(doc -> {
                    // 计算并保存元数据结果。
                    Map<String, Object> metadata = doc.getMetadata();
                    // 计算并保存文件name结果。
                    String fileName = metadata.getOrDefault("file_name", "unknown").toString();
                    // 计算并保存url结果。
                    String url = metadata.getOrDefault("file_url", "").toString();
                    // 返回组装完成的结果对象。
                    return SourceReference.builder()
                            // 设置文件name字段的取值。
                            .fileName(fileName)
                            // 设置url字段的取值。
                            .url(url)
                            // 完成当前建造者对象的组装。
                            .build();
                })
                // 设置filter字段的取值。
                .filter(source -> {
                    // 根据条件判断当前分支是否执行。
                    if (seenUrls.contains(source.getUrl())) {
                        // 返回false。
                        return false;
                    }
                    // 向当前集合中追加元素。
                    seenUrls.add(source.getUrl());
                    // 返回true。
                    return true;
                })
                // 设置limit字段的取值。
                .limit(3)
                // 设置tolist字段的取值。
                .toList();
    }

    /**
     * 根据请求中的会话标识解析当前会话实体。
     * @param sessionId 会话id参数。
     * @return 匹配到的会话实体。
     */
    private ChatSession resolveSession(String sessionId) {
        // 根据条件判断当前分支是否执行。
        if (!StringUtils.hasText(sessionId)) {
            // 返回空值，表示当前没有可用结果。
            return null;
        }
        // 返回 `getSessionEntity` 的处理结果。
        return chatSessionService.getSessionEntity(sessionId.trim());
    }

    /**
     * 将本轮用户提问和助手回答保存到会话消息表。
     * @param session 会话参数。
     * @param question 问题参数。
     * @param answer 回答参数。
     * @param firstTurn firstturn参数。
     */
    private void persistConversation(ChatSession session, String question, String answer, boolean firstTurn) {
        // 围绕问答消息repository补充当前业务语句。
        chatMessageRepository.save(ChatMessage.builder()
                // 设置会话字段的取值。
                .session(session)
                // 设置role字段的取值。
                .role("user")
                // 设置内容字段的取值。
                .content(question)
                // 完成当前建造者对象的组装。
                .build());

        // 围绕问答消息repository补充当前业务语句。
        chatMessageRepository.save(ChatMessage.builder()
                // 设置会话字段的取值。
                .session(session)
                // 设置role字段的取值。
                .role("assistant")
                // 设置内容字段的取值。
                .content(answer)
                // 完成当前建造者对象的组装。
                .build());

        // 根据条件判断当前分支是否执行。
        if (firstTurn && "New Session".equals(session.getTitle())) {
            // 更新标题字段。
            session.setTitle(buildSessionTitle(question));
        }
        // 调用 `touchSession` 完成当前步骤。
        chatSessionService.touchSession(session);
    }

    /**
     * 根据首轮问题内容生成简短的会话标题。
     * @param question 问题参数。
     * @return 处理后的字符串结果。
     */
    private String buildSessionTitle(String question) {
        // 清理并规范化normalized内容。
        String normalized = question.replaceAll("\\s+", " ").trim();
        // 根据条件判断当前分支是否执行。
        if (normalized.length() <= 20) {
            // 返回normalized。
            return normalized;
        }
        // 返回 `substring` 的处理结果。
        return normalized.substring(0, 20) + "...";
    }
}
