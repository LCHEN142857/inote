// 声明当前源文件所属包。
package com.inote.service.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 处理检索结果相关逻辑。
 * @param originalQuery original查询参数。
 * @param searchQuery search查询参数。
 * @param documents 文档参数。
 */
public record RetrievalResult(
        String originalQuery,
        String searchQuery,
        List<Document> documents
) {
}
