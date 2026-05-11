package com.inote.service.retrieval;

import com.inote.service.ChatModelSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private static final String REWRITE_PROMPT = """
            你是一个查询优化专家。请将用户的口语化问题改写为更专业、完整、适合信息检索的查询语句。
            要求：
            1. 保持原始查询的核心意图不变
            2. 补充可能被省略的关键词和上下文
            3. 使用更专业准确的术语
            4. 只输出改写后的查询，不要添加任何解释
            """;

    private final ChatModel chatModel;
    private final ChatModelSelectionService chatModelSelectionService;

    public String rewrite(String originalQuery) {
        return rewrite(originalQuery, null);
    }

    public String rewrite(String originalQuery, String modelName) {
        try {
            log.debug("Rewriting query: {}", originalQuery);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(REWRITE_PROMPT),
                    new UserMessage(originalQuery)
            ), chatModelSelectionService.buildOptions(modelName));

            ChatResponse response = chatModel.call(prompt);
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String rewritten = response.getResult().getOutput().getText().trim();
                if (!rewritten.isEmpty()) {
                    log.info("Query rewritten: '{}' -> '{}'", originalQuery, rewritten);
                    return rewritten;
                }
            }

            log.warn("Query rewrite returned empty result, using original query");
            return originalQuery;
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original query: {}", e.getMessage());
            return originalQuery;
        }
    }
}
