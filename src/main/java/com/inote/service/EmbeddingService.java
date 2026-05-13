package com.inote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public void embedAndStore(List<String> chunks, Map<String, Object> metadata) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks to embed");
            return;
        }

        log.info("Embedding {} chunks", chunks.size());

        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> docMetadata = new HashMap<>(metadata);
                    docMetadata.put("chunk_size", chunk.length());
                    return new Document(chunk, docMetadata);
                })
                .toList();

        vectorStore.add(documents);
        log.info("Successfully stored {} documents to vector store", documents.size());
    }

    public List<Document> searchSimilarDocuments(String query, int topK) {
        return searchSimilarDocuments(query, topK, 0.0, null);
    }

    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold) {
        return searchSimilarDocuments(query, topK, similarityThreshold, null);
    }

    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold, String ownerId) {
        return searchSimilarDocuments(query, topK, similarityThreshold, ownerId, null);
    }

    public List<Document> searchSimilarDocuments(String query, int topK, double similarityThreshold, String ownerId, String documentId) {
        log.debug("Searching similar documents for query: {}, topK: {}, threshold: {}, ownerId: {}, documentId: {}",
                query, topK, similarityThreshold, ownerId, documentId);

        SearchRequest searchRequest = buildSearchRequest(query, topK, similarityThreshold, ownerId, documentId);
        List<Document> results = vectorStore.similaritySearch(searchRequest);

        if (results.isEmpty() && similarityThreshold > 0.0) {
            log.debug("No results with threshold {}, retrying with threshold 0.0", similarityThreshold);
            results = vectorStore.similaritySearch(buildSearchRequest(query, topK, 0.0, ownerId, documentId));
        }

        log.debug("Found {} similar documents", results.size());
        return results;
    }

    private SearchRequest buildSearchRequest(String query, int topK, double similarityThreshold, String ownerId) {
        return buildSearchRequest(query, topK, similarityThreshold, ownerId, null);
    }

    private SearchRequest buildSearchRequest(String query, int topK, double similarityThreshold, String ownerId, String documentId) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

        if (similarityThreshold > 0.0) {
            builder.similarityThreshold(similarityThreshold);
        }

        List<String> filters = new ArrayList<>();
        if (StringUtils.hasText(ownerId)) {
            filters.add("owner_id == '" + ownerId + "'");
        }
        if (StringUtils.hasText(documentId)) {
            filters.add("document_id == '" + documentId + "'");
        }
        if (!filters.isEmpty()) {
            builder.filterExpression(String.join(" && ", filters));
        }

        return builder.build();
    }

    public float[] embed(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        if (response.getResults() != null && !response.getResults().isEmpty()) {
            return response.getResults().get(0).getOutput();
        }
        return new float[0];
    }
}
