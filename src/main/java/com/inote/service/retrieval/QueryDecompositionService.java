// 声明当前源文件的包。
package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class QueryDecompositionService {

    // 声明当前字段。
    private final ChatModel chatModel;
    // 声明当前字段。
    private final RagProperties ragProperties;

    private static final String DECOMPOSITION_PROMPT = """
            你是一个查询分解专家。请分析以下查询，判断是否需要拆分为多个子查询进行独立检索。

            规则：
            1. 如果查询涉及多个不同主题或方面，将其拆分为2-4个独立的子查询
            2. 如果查询已经足够简单明确，返回"无需拆分"
            3. 每个子查询应该是一个完整的、可独立检索的问题
            4. 用数字编号列出子查询，每行一个
            """;

    /**
     * 描述 `Pattern.compile` 操作。
     *
     * @param "^\\s*\\d+ 输入参数 `"^\\s*\\d+`。
     * @return 类型为 `Pattern NUMBERED_PATTERN =` 的返回值。
     */
    // 执行当前语句。
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+[.、)）]\\s*(.+)$");

    /**
     * 描述 `decompose` 操作。
     *
     * @param query 输入参数 `query`。
     * @return 类型为 `List<String>` 的返回值。
     */
    // 处理当前代码结构。
    public List<String> decompose(String query) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            log.debug("Decomposing query: {}", query);

            // 处理当前代码结构。
            Prompt prompt = new Prompt(List.of(
                    // 处理当前代码结构。
                    new SystemMessage(DECOMPOSITION_PROMPT),
                    // 处理当前代码结构。
                    new UserMessage(query)
            // 执行当前语句。
            ));

            // 执行当前语句。
            ChatResponse response = chatModel.call(prompt);
            // 执行当前流程控制分支。
            if (response.getResult() == null || response.getResult().getOutput() == null) {
                // 执行当前语句。
                log.warn("Query decomposition returned empty response");
                // 返回当前结果。
                return List.of(query);
            // 结束当前代码块。
            }

            // 执行当前语句。
            String text = response.getResult().getOutput().getText().trim();

            // 执行当前流程控制分支。
            if (text.contains("无需拆分")) {
                // 执行当前语句。
                log.debug("Query does not need decomposition");
                // 返回当前结果。
                return List.of(query);
            // 结束当前代码块。
            }

            // 执行当前语句。
            List<String> subQueries = parseSubQueries(text);

            // 执行当前流程控制分支。
            if (subQueries.isEmpty()) {
                // 执行当前语句。
                log.debug("No sub-queries parsed, using original query");
                // 返回当前结果。
                return List.of(query);
            // 结束当前代码块。
            }

            // 执行当前语句。
            int maxSub = ragProperties.getMaxSubQueries();
            // 执行当前流程控制分支。
            if (subQueries.size() > maxSub) {
                // 执行当前语句。
                subQueries = subQueries.subList(0, maxSub);
            // 结束当前代码块。
            }

            // 执行当前流程控制分支。
            if (!subQueries.contains(query)) {
                // 执行当前语句。
                subQueries.add(0, query);
                // 执行当前流程控制分支。
                if (subQueries.size() > maxSub) {
                    // 执行当前语句。
                    subQueries = subQueries.subList(0, maxSub);
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }

            // 执行当前语句。
            log.info("Query decomposed into {} sub-queries: {}", subQueries.size(), subQueries);
            // 返回当前结果。
            return subQueries;

        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.warn("Query decomposition failed, using original query: {}", e.getMessage());
            // 返回当前结果。
            return List.of(query);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `parseSubQueries` 操作。
     *
     * @param text 输入参数 `text`。
     * @return 类型为 `List<String>` 的返回值。
     */
    // 处理当前代码结构。
    private List<String> parseSubQueries(String text) {
        // 执行当前语句。
        List<String> subQueries = new ArrayList<>();
        // 执行当前语句。
        String[] lines = text.split("\\n");

        // 执行当前流程控制分支。
        for (String line : lines) {
            // 执行当前语句。
            line = line.trim();
            // 执行当前流程控制分支。
            if (line.isEmpty()) {
                // 执行当前语句。
                continue;
            // 结束当前代码块。
            }
            // 执行当前语句。
            Matcher matcher = NUMBERED_PATTERN.matcher(line);
            // 执行当前流程控制分支。
            if (matcher.matches()) {
                // 执行当前语句。
                String subQuery = matcher.group(1).trim();
                // 执行当前流程控制分支。
                if (!subQuery.isEmpty()) {
                    // 执行当前语句。
                    subQueries.add(subQuery);
                // 结束当前代码块。
                }
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }

        // 返回当前结果。
        return subQueries;
    // 结束当前代码块。
    }
// 结束当前代码块。
}
