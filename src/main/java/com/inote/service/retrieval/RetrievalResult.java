package com.inote.service.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

public record RetrievalResult(
        String originalQuery,
        String searchQuery,
        List<Document> documents
) {
}
