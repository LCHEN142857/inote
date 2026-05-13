package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalPipelineService {

    private final RagProperties ragProperties;
    private final QueryRewriteService queryRewriteService;
    private final QueryDecompositionService queryDecompositionService;
    private final HybridRetrievalService hybridRetrievalService;
    private final RerankService rerankService;

    public RetrievalResult retrieve(String originalQuery) {
        return retrieve(originalQuery, null);
    }

    public RetrievalResult retrieve(String originalQuery, String modelName) {
        log.info("Starting retrieval pipeline for query: {}", originalQuery);

        String searchQuery = originalQuery;
        // if (ragProperties.isQueryRewriteEnabled()) {
        //     searchQuery = queryRewriteService.rewrite(originalQuery, modelName);
        // }
        //
        // List<String> queries;
        // if (ragProperties.isMultiQueryEnabled()) {
        //     queries = queryDecompositionService.decompose(searchQuery, modelName);
        // } else {
        //     queries = List.of(searchQuery);
        // }

        List<Document> allCandidates = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        List<Document> results = hybridRetrievalService.retrieve(searchQuery);
        for (Document doc : results) {
            if (seenIds.add(doc.getId())) {
                allCandidates.add(doc);
            }
        }
        // for (String query : queries) {
        //     List<Document> results = hybridRetrievalService.retrieve(query);
        //     for (Document doc : results) {
        //         if (seenIds.add(doc.getId())) {
        //             allCandidates.add(doc);
        //         }
        //     }
        // }

        List<Document> finalDocs;
        if (ragProperties.isRerankEnabled() && allCandidates.size() > 1) {
            finalDocs = rerankService.rerank(originalQuery, allCandidates, ragProperties.getRerankTopN());
        } else {
            finalDocs = allCandidates.stream()
                    .limit(ragProperties.getFinalTopK())
                    .collect(Collectors.toList());
        }

        log.info("Retrieval pipeline completed, {} documents returned", finalDocs.size());
        return new RetrievalResult(originalQuery, searchQuery, finalDocs);
    }
}
