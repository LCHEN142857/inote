// 声明当前源文件的包。
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

// 应用当前注解。
@ExtendWith(MockitoExtension.class)
// 声明当前类型。
class DocumentServiceTest {

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private DocumentRepository documentRepository;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private DocumentProcessingService processingService;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private CurrentUserService currentUserService;

    // 应用当前注解。
    @InjectMocks
    // 声明当前字段。
    private DocumentService documentService;

    // 声明当前字段。
    private User currentUser;

    /**
     * 描述 `setUp` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @BeforeEach
    // 处理当前代码结构。
    void setUp() throws Exception {
        // 执行当前语句。
        currentUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 执行当前语句。
        ReflectionTestUtils.setField(documentService, "uploadPath", "./target/test-uploads");
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentPersistsDocumentAndTriggersAsyncProcessing` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentPersistsDocumentAndTriggersAsyncProcessing() throws Exception {
        // 执行当前语句。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello world".getBytes());
        // 执行当前语句。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 执行当前语句。
        when(processingService.saveFile(file, "./target/test-uploads")).thenReturn(Path.of("./target/test-uploads/saved.txt"));
        // 处理当前代码结构。
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            // 执行当前语句。
            Document document = invocation.getArgument(0, Document.class);
            // 执行当前流程控制分支。
            if (document.getId() == null) {
                // 执行当前语句。
                document.setId("doc-1");
            // 结束当前代码块。
            }
            // 返回当前结果。
            return document;
        // 执行当前语句。
        });
        // 执行当前语句。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 执行当前语句。
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        // 执行当前语句。
        verify(documentRepository, times(2)).save(captor.capture());
        // 执行当前语句。
        assertThat(response.getStatus()).isEqualTo("PARSING");
        // 执行当前语句。
        assertThat(response.getDocumentId()).isEqualTo("doc-1");
        // 执行当前语句。
        verify(processingService).processDocumentAsync(any(Document.class), eq(file), eq("/api/v1/documents/files/doc-1"));
        // 执行当前语句。
        assertThat(captor.getAllValues().get(1).getFileUrl()).isEqualTo("/api/v1/documents/files/doc-1");
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentReturnsFailedStatusWhenExtensionIsUnsupported` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentReturnsFailedStatusWhenExtensionIsUnsupported() throws Exception {
        // 执行当前语句。
        MockMultipartFile file = new MockMultipartFile("file", "sample.exe", "application/octet-stream", "hello".getBytes());
        // 执行当前语句。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 执行当前语句。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 执行当前语句。
        assertThat(response.getMessage()).contains("Unsupported file type");
        // 执行当前语句。
        verify(documentRepository, never()).save(any(Document.class));
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatusThrowsWhenOwnedDocumentDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws EntityNotFoundException 已声明的异常类型 `EntityNotFoundException`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getDocumentStatusThrowsWhenOwnedDocumentDoesNotExist() throws EntityNotFoundException {
        // 执行当前语句。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 执行当前语句。
        when(documentRepository.findByIdAndOwnerId("missing", "user-1")).thenReturn(Optional.empty());
        // 执行当前语句。
        assertThatThrownBy(() -> documentService.getDocumentStatus("missing")).isInstanceOf(EntityNotFoundException.class).hasMessage("Document not found: missing");
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocumentsMapsRepositoryEntitiesToStatusResponses` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listDocumentsMapsRepositoryEntitiesToStatusResponses() throws Exception {
        // 执行当前语句。
        Document completedDocument = TestDataFactory.document("doc-1", currentUser, "COMPLETED");
        // 执行当前语句。
        completedDocument.setUpdatedAt(LocalDateTime.of(2026, 4, 16, 12, 30));
        // 执行当前语句。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 执行当前语句。
        when(documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(completedDocument));
        // 执行当前语句。
        List<DocumentStatusResponse> responses = documentService.listDocuments();
        // 执行当前语句。
        assertThat(responses).hasSize(1);
        // 执行当前语句。
        assertThat(responses.get(0).getDocumentId()).isEqualTo("doc-1");
        // 执行当前语句。
        assertThat(responses.get(0).getStatus()).isEqualTo("COMPLETED");
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentReturnsFailedStatusWhenSavingFileThrowsIOException` 操作。
     *
     * @return 无返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentReturnsFailedStatusWhenSavingFileThrowsIOException() throws IOException {
        // 执行当前语句。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());
        // 执行当前语句。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 执行当前语句。
        when(processingService.saveFile(file, "./target/test-uploads")).thenThrow(new IOException("disk full"));
        // 执行当前语句。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 执行当前语句。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 执行当前语句。
        assertThat(response.getMessage()).contains("File save failed");
    // 结束当前代码块。
    }
// 结束当前代码块。
}
