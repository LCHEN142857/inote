package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import com.inote.repository.DocumentRepository;
import com.inote.security.CurrentUserService;
import com.inote.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final EmbeddingService embeddingService;
    private final BM25SearchService bm25SearchService;
    private final RagProperties ragProperties;
    private final CurrentUserService currentUserService;
    private final DocumentRepository documentRepository;

    public List<Document> retrieve(String query) {
        int vectorTopK = ragProperties.getVectorTopK();
        String ownerId = currentUserService.getCurrentUser().getId();

        List<Document> vectorResults = embeddingService.searchSimilarDocuments(
                query, vectorTopK, ragProperties.getSimilarityThreshold(), ownerId);
        log.debug("Vector search returned {} results", vectorResults.size());

        if (!ragProperties.isHybridSearchEnabled()) {
            Set<String> activeDocumentIds = resolveActiveDocumentIds(ownerId, collectCandidateDocumentIds(vectorResults, List.<BM25SearchService.BM25Result>of()));
            return filterActiveVectorResults(vectorResults, activeDocumentIds);
        }

        int bm25TopK = ragProperties.getBm25TopK();
        List<BM25SearchService.BM25Result> bm25Results;
        try {
            bm25Results = bm25SearchService.search(query, bm25TopK, ownerId);
            log.debug("BM25 search returned {} results", bm25Results.size());
        } catch (Exception e) {
            log.warn("BM25 search failed, using vector-only results: {}", e.getMessage());
            Set<String> activeDocumentIds = resolveActiveDocumentIds(ownerId, collectCandidateDocumentIds(vectorResults, List.<BM25SearchService.BM25Result>of()));
            return filterActiveVectorResults(vectorResults, activeDocumentIds);
        }

        Set<String> activeDocumentIds = resolveActiveDocumentIds(ownerId, collectCandidateDocumentIds(vectorResults, bm25Results));
        return fuseWithRRF(
                filterActiveVectorResults(vectorResults, activeDocumentIds),
                filterActiveBm25Results(bm25Results, activeDocumentIds)
        );
    }

    private Set<String> collectCandidateDocumentIds(
            List<Document> vectorResults,
            List<BM25SearchService.BM25Result> bm25Results) {

        Set<String> candidateDocumentIds = new HashSet<>();
        vectorResults.stream()
                .map(this::extractDocumentId)
                .filter(this::hasText)
                .forEach(candidateDocumentIds::add);
        bm25Results.stream()
                .map(result -> extractDocumentId(result.metadata()))
                .filter(this::hasText)
                .forEach(candidateDocumentIds::add);

        return candidateDocumentIds;
    }

    private List<Document> filterActiveVectorResults(List<Document> vectorResults, Set<String> activeDocumentIds) {
        return vectorResults.stream()
                .filter(document -> activeDocumentIds.contains(extractDocumentId(document)))
                .toList();
    }

    private List<BM25SearchService.BM25Result> filterActiveBm25Results(
            List<BM25SearchService.BM25Result> bm25Results,
            Set<String> activeDocumentIds) {

        return bm25Results.stream()
                .filter(result -> activeDocumentIds.contains(extractDocumentId(result.metadata())))
                .toList();
    }

    private Set<String> resolveActiveDocumentIds(String ownerId, Set<String> candidateDocumentIds) {
        if (candidateDocumentIds.isEmpty()) {
            return Set.of();
        }

        return documentRepository.findAllByIdInAndOwnerId(List.copyOf(candidateDocumentIds), ownerId).stream()
                .filter(this::isActiveVersion)
                .map(com.inote.model.entity.Document::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String extractDocumentId(Document document) {
        return extractDocumentId(document.getMetadata());
    }

    private String extractDocumentId(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get("document_id");
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isActiveVersion(com.inote.model.entity.Document document) {
        return !Boolean.FALSE.equals(document.getActive());
    }

    private List<Document> fuseWithRRF(
            List<Document> vectorResults,
            List<BM25SearchService.BM25Result> bm25Results) {

        double k = ragProperties.getRrfK();
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Document> docMap = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String id = doc.getId();
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, doc);
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            BM25SearchService.BM25Result bm25 = bm25Results.get(i);
            String id = bm25.documentId();
            rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, new Document(id, bm25.content(), bm25.metadata()));
        }

        List<Document> fused = new ArrayList<>(docMap.values());
        fused.sort((a, b) -> Double.compare(
                rrfScores.getOrDefault(b.getId(), 0.0),
                rrfScores.getOrDefault(a.getId(), 0.0)));

        log.debug("RRF fusion produced {} unique documents", fused.size());
        return fused;
    }
}
