// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义查询拆分服务，负责将复杂问题拆成多个子查询。
public class QueryDecompositionService {

    // 声明问答模型变量，供后续流程使用。
    private final ChatModel chatModel;
    // 声明ragproperties变量，供后续流程使用。
    private final RagProperties ragProperties;

    // 写入当前请求体或提示词模板内容。
    private static final String DECOMPOSITION_PROMPT = """
            你是一个查询分解专家。请分析以下查询，判断是否需要拆分为多个子查询进行独立检索。

            规则：
            1. 如果查询涉及多个不同主题或方面，将其拆分为2-4个独立的子查询
            2. 如果查询已经足够简单明确，返回"无需拆分"
            3. 每个子查询应该是一个完整的、可独立检索的问题
            4. 用数字编号列出子查询，每行一个
            """;

    // 计算并保存numberedpattern结果。
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+[.、)）]\\s*(.+)$");

    /**
     * 调用模型拆分复杂查询，扩展检索覆盖面。
     * @param query 查询参数。
     * @return 列表形式的处理结果。
     */
    public List<String> decompose(String query) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 记录当前流程的运行日志。
            log.debug("Decomposing query: {}", query);

            // 围绕提示词提示词提示词补充当前业务语句。
            Prompt prompt = new Prompt(List.of(
                    // 围绕system消息拆分补充当前业务语句。
                    new SystemMessage(DECOMPOSITION_PROMPT),
                    // 围绕用户消息查询补充当前业务语句。
                    new UserMessage(query)
            // 继续补全当前链式调用或多行表达式。
            ));

            // 调用模型生成响应结果。
            ChatResponse response = chatModel.call(prompt);
            // 根据条件判断当前分支是否执行。
            if (response.getResult() == null || response.getResult().getOutput() == null) {
                // 记录当前流程的运行日志。
                log.warn("Query decomposition returned empty response");
                // 返回固定列表结果。
                return List.of(query);
            }

            // 清理并规范化text内容。
            String text = response.getResult().getOutput().getText().trim();

            // 根据条件判断当前分支是否执行。
            if (text.contains("无需拆分")) {
                // 记录当前流程的运行日志。
                log.debug("Query does not need decomposition");
                // 返回固定列表结果。
                return List.of(query);
            }

            // 计算并保存子queries结果。
            List<String> subQueries = parseSubQueries(text);

            // 根据条件判断当前分支是否执行。
            if (subQueries.isEmpty()) {
                // 记录当前流程的运行日志。
                log.debug("No sub-queries parsed, using original query");
                // 返回固定列表结果。
                return List.of(query);
            }

            // 计算并保存max子结果。
            int maxSub = ragProperties.getMaxSubQueries();
            // 根据条件判断当前分支是否执行。
            if (subQueries.size() > maxSub) {
                // 计算并保存子queries结果。
                subQueries = subQueries.subList(0, maxSub);
            }

            // 根据条件判断当前分支是否执行。
            if (!subQueries.contains(query)) {
                // 向当前集合中追加元素。
                subQueries.add(0, query);
                // 根据条件判断当前分支是否执行。
                if (subQueries.size() > maxSub) {
                    // 计算并保存子queries结果。
                    subQueries = subQueries.subList(0, maxSub);
                }
            }

            // 记录当前流程的运行日志。
            log.info("Query decomposed into {} sub-queries: {}", subQueries.size(), subQueries);
            // 返回子queries。
            return subQueries;

        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.warn("Query decomposition failed, using original query: {}", e.getMessage());
            // 返回固定列表结果。
            return List.of(query);
        }
    }

    /**
     * 解析模型返回的编号子查询列表。
     * @param text text参数。
     * @return 列表形式的处理结果。
     */
    private List<String> parseSubQueries(String text) {
        // 创建子queries对象。
        List<String> subQueries = new ArrayList<>();
        // 计算并保存lines结果。
        String[] lines = text.split("\\n");

        // 遍历当前集合或区间中的元素。
        for (String line : lines) {
            // 清理并规范化line内容。
            line = line.trim();
            // 根据条件判断当前分支是否执行。
            if (line.isEmpty()) {
                // 跳过当前循环剩余逻辑，进入下一轮迭代。
                continue;
            }
            // 计算并保存matcher结果。
            Matcher matcher = NUMBERED_PATTERN.matcher(line);
            // 根据条件判断当前分支是否执行。
            if (matcher.matches()) {
                // 清理并规范化子查询内容。
                String subQuery = matcher.group(1).trim();
                // 根据条件判断当前分支是否执行。
                if (!subQuery.isEmpty()) {
                    // 向当前集合中追加元素。
                    subQueries.add(subQuery);
                }
            }
        }

        // 返回子queries。
        return subQueries;
    }
}
