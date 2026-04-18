// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义查询改写服务，负责用模型优化检索查询。
public class QueryRewriteService {

    // 声明问答模型变量，供后续流程使用。
    private final ChatModel chatModel;

    // 写入当前请求体或提示词模板内容。
    private static final String REWRITE_PROMPT = """
            你是一个查询优化专家。请将用户的口语化问题改写为更专业、完整、适合信息检索的查询语句。

            要求：
            1. 保持原始查询的核心意图不变
            2. 补充可能被省略的关键词和上下文
            3. 使用更专业准确的术语
            4. 只输出改写后的查询，不要添加任何解释
            """;

    /**
     * 调用模型改写原始查询，提升后续检索命中率。
     * @param originalQuery original查询参数。
     * @return 处理后的字符串结果。
     */
    public String rewrite(String originalQuery) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 记录当前流程的运行日志。
            log.debug("Rewriting query: {}", originalQuery);

            // 围绕提示词提示词提示词补充当前业务语句。
            Prompt prompt = new Prompt(List.of(
                    // 围绕system消息改写补充当前业务语句。
                    new SystemMessage(REWRITE_PROMPT),
                    // 围绕用户消息original补充当前业务语句。
                    new UserMessage(originalQuery)
            // 继续补全当前链式调用或多行表达式。
            ));

            // 调用模型生成响应结果。
            ChatResponse response = chatModel.call(prompt);
            // 根据条件判断当前分支是否执行。
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                // 清理并规范化rewritten内容。
                String rewritten = response.getResult().getOutput().getText().trim();
                // 根据条件判断当前分支是否执行。
                if (!rewritten.isEmpty()) {
                    // 记录当前流程的运行日志。
                    log.info("Query rewritten: '{}' -> '{}'", originalQuery, rewritten);
                    // 返回rewritten。
                    return rewritten;
                }
            }

            // 记录当前流程的运行日志。
            log.warn("Query rewrite returned empty result, using original query");
            // 返回original查询。
            return originalQuery;
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("Query rewrite failed, using original query: {}", e.getMessage());
            // 返回original查询。
            return originalQuery;
        }
    }
}
