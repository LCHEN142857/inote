package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import com.inote.service.ChatModelSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryDecompositionService {

    private static final String DECOMPOSITION_PROMPT = """
            你是一个查询分解专家。请分析以下查询，判断是否需要拆分为多个子查询进行独立检索。
            规则：
            1. 如果查询涉及多个不同主题或方面，将其拆分为2-4个独立的子查询
            2. 如果查询已经足够简单明确，返回"无需拆分"
            3. 每个子查询应该是一个完整的、可独立检索的问题
            4. 用数字编号列出子查询，每行一个
            """;

    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+[.、)\\-]\\s*(.+)$");

    private final ChatModel chatModel;
    private final RagProperties ragProperties;
    private final ChatModelSelectionService chatModelSelectionService;

    public List<String> decompose(String query) {
        return decompose(query, null);
    }

    public List<String> decompose(String query, String modelName) {
        try {
            log.debug("Decomposing query: {}", query);

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(DECOMPOSITION_PROMPT),
                    new UserMessage(query)
            ), chatModelSelectionService.buildOptions(modelName));

            ChatResponse response = chatModel.call(prompt);
            if (response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("Query decomposition returned empty response");
                return List.of(query);
            }

            String text = response.getResult().getOutput().getText().trim();
            if (text.contains("无需拆分")) {
                log.debug("Query does not need decomposition");
                return List.of(query);
            }

            List<String> subQueries = parseSubQueries(text);
            if (subQueries.isEmpty()) {
                log.debug("No sub-queries parsed, using original query");
                return List.of(query);
            }

            int maxSub = ragProperties.getMaxSubQueries();
            if (subQueries.size() > maxSub) {
                subQueries = subQueries.subList(0, maxSub);
            }

            if (!subQueries.contains(query)) {
                subQueries.add(0, query);
                if (subQueries.size() > maxSub) {
                    subQueries = subQueries.subList(0, maxSub);
                }
            }

            log.info("Query decomposed into {} sub-queries: {}", subQueries.size(), subQueries);
            return subQueries;
        } catch (Exception e) {
            log.warn("Query decomposition failed, using original query: {}", e.getMessage());
            return List.of(query);
        }
    }

    private List<String> parseSubQueries(String text) {
        List<String> subQueries = new ArrayList<>();
        String[] lines = text.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = NUMBERED_PATTERN.matcher(line);
            if (matcher.matches()) {
                String subQuery = matcher.group(1).trim();
                if (!subQuery.isEmpty()) {
                    subQueries.add(subQuery);
                }
            }
        }

        return subQueries;
    }
}
