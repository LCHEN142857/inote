// 声明当前源文件的包。
package com.inote.service.retrieval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class QueryRewriteService {

    // 声明当前字段。
    private final ChatModel chatModel;

    private static final String REWRITE_PROMPT = """
            你是一个查询优化专家。请将用户的口语化问题改写为更专业、完整、适合信息检索的查询语句。

            要求：
            1. 保持原始查询的核心意图不变
            2. 补充可能被省略的关键词和上下文
            3. 使用更专业准确的术语
            4. 只输出改写后的查询，不要添加任何解释
            """;

    /**
     * 描述 `rewrite` 操作。
     *
     * @param originalQuery 输入参数 `originalQuery`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    public String rewrite(String originalQuery) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            log.debug("Rewriting query: {}", originalQuery);

            // 处理当前代码结构。
            Prompt prompt = new Prompt(List.of(
                    // 处理当前代码结构。
                    new SystemMessage(REWRITE_PROMPT),
                    // 处理当前代码结构。
                    new UserMessage(originalQuery)
            // 执行当前语句。
            ));

            // 执行当前语句。
            ChatResponse response = chatModel.call(prompt);
            // 执行当前流程控制分支。
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                // 执行当前语句。
                String rewritten = response.getResult().getOutput().getText().trim();
                // 执行当前流程控制分支。
                if (!rewritten.isEmpty()) {
                    // 执行当前语句。
                    log.info("Query rewritten: '{}' -> '{}'", originalQuery, rewritten);
                    // 返回当前结果。
                    return rewritten;
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }

            // 执行当前语句。
            log.warn("Query rewrite returned empty result, using original query");
            // 返回当前结果。
            return originalQuery;
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("Query rewrite failed, using original query: {}", e.getMessage());
            // 返回当前结果。
            return originalQuery;
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }
// 结束当前代码块。
}
