// 声明当前源文件的包。
package com.inote.service.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

// 声明当前类型。
public record RetrievalResult(
        // 处理当前代码结构。
        String originalQuery,
        // 处理当前代码结构。
        String searchQuery,
        // 处理当前代码结构。
        List<Document> documents
// 处理当前代码结构。
) {
// 结束当前代码块。
}
