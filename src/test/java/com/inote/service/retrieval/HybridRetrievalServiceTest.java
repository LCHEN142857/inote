package com.inote.service.retrieval;

import com.inote.config.RagProperties;
import com.inote.repository.DocumentRepository;
import com.inote.security.CurrentUserService;
import com.inote.service.EmbeddingService;
import com.inote.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private BM25SearchService bm25SearchService;

    @Mock
    private RagProperties ragProperties;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private HybridRetrievalService hybridRetrievalService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUser()).thenReturn(TestDataFactory.user("user-1", "tester", "token-1"));
        when(ragProperties.getVectorTopK()).thenReturn(5);
        when(ragProperties.getSimilarityThreshold()).thenReturn(0.5);
        when(ragProperties.isHybridSearchEnabled()).thenReturn(true);
        when(ragProperties.getBm25TopK()).thenReturn(5);
        when(ragProperties.getRrfK()).thenReturn(60.0);
    }

    @Test
    void retrieveDropsInactiveDocumentVersionsBeforeFusion() {
        Document activeChunk = Document.builder()
                .id("chunk-active")
                .text("active content")
                .metadata(Map.of("document_id", "doc-active", "file_name", "active.txt", "owner_id", "user-1"))
                .build();
        Document inactiveChunk = Document.builder()
                .id("chunk-inactive")
                .text("inactive content")
                .metadata(Map.of("document_id", "doc-inactive", "file_name", "inactive.txt", "owner_id", "user-1"))
                .build();

        when(embeddingService.searchSimilarDocuments("question", 5, 0.5, "user-1"))
                .thenReturn(List.of(activeChunk, inactiveChunk));
        when(bm25SearchService.search("question", 5, "user-1"))
                .thenReturn(List.of(
                        new BM25SearchService.BM25Result("bm25-active", "active bm25", Map.of("document_id", "doc-active"), 1.0),
                        new BM25SearchService.BM25Result("bm25-inactive", "inactive bm25", Map.of("document_id", "doc-inactive"), 0.9)
                ));
        var activeDocument = TestDataFactory.document("doc-active", TestDataFactory.user("user-1", "tester", "token-1"), "COMPLETED");
        var inactiveDocument = TestDataFactory.document("doc-inactive", TestDataFactory.user("user-1", "tester", "token-1"), "COMPLETED");
        inactiveDocument.setActive(Boolean.FALSE);
        when(documentRepository.findAllByIdInAndOwnerId(anyList(), eq("user-1")))
                .thenReturn(List.of(activeDocument, inactiveDocument));

        List<Document> results = hybridRetrievalService.retrieve("question");

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(doc -> doc.getMetadata().get("document_id"))
                .containsOnly("doc-active");
    }
}
