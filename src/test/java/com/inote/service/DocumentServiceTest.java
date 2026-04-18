package com.inote.service;

import com.inote.model.dto.DocumentStatusResponse;
import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;
import com.inote.repository.DocumentRepository;
import com.inote.security.CurrentUserService;
import com.inote.support.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentProcessingService processingService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DocumentService documentService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = TestDataFactory.user("user-1", "tester", "token-1");
        ReflectionTestUtils.setField(documentService, "uploadPath", "./target/test-uploads");
    }

    @Test
    void uploadDocumentPersistsDocumentAndTriggersAsyncProcessing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello world".getBytes());
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(processingService.saveFile(file, "./target/test-uploads"))
                .thenReturn(Path.of("./target/test-uploads/saved.txt"));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0, Document.class);
            if (document.getId() == null) {
                document.setId("doc-1");
            }
            return document;
        });

        DocumentUploadResponse response = documentService.uploadDocument(file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, times(2)).save(captor.capture());
        verify(processingService).processDocumentAsync("doc-1", "/api/v1/documents/files/doc-1");
        assertThat(response.getStatus()).isEqualTo("PARSING");
        assertThat(response.getDocumentId()).isEqualTo("doc-1");
        assertThat(captor.getAllValues().get(1).getFileUrl()).isEqualTo("/api/v1/documents/files/doc-1");
    }

    @Test
    void uploadDocumentRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.exe", "application/octet-stream", "hello".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadDocumentRejectsLegacyDocFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.doc", "application/msword", "hello".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type: doc");
    }

    @Test
    void getDocumentStatusThrowsWhenOwnedDocumentDoesNotExist() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(documentRepository.findByIdAndOwnerId("missing", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentStatus("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Document not found: missing");
    }

    @Test
    void listDocumentsMapsRepositoryEntitiesToStatusResponses() {
        Document completedDocument = TestDataFactory.document("doc-1", currentUser, "COMPLETED");
        completedDocument.setUpdatedAt(LocalDateTime.of(2026, 4, 16, 12, 30));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(completedDocument));

        List<DocumentStatusResponse> responses = documentService.listDocuments();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getDocumentId()).isEqualTo("doc-1");
        assertThat(responses.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void uploadDocumentThrowsStorageExceptionWhenSavingFileFails() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(processingService.saveFile(file, "./target/test-uploads")).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> documentService.uploadDocument(file))
                .isInstanceOf(DocumentStorageException.class)
                .hasMessage("File save failed.");
    }
}
