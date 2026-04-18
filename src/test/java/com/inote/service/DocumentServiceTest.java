// 声明当前源文件所属包。
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

// 为当前测试类启用指定扩展。
@ExtendWith(MockitoExtension.class)
// 定义 `DocumentServiceTest` 类型。
class DocumentServiceTest {

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明文档repository变量，供后续流程使用。
    private DocumentRepository documentRepository;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明processingservice变量，供后续流程使用。
    private DocumentProcessingService processingService;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明当前用户service变量，供后续流程使用。
    private CurrentUserService currentUserService;

    // 将模拟依赖注入被测对象。
    @InjectMocks
    // 声明文档service变量，供后续流程使用。
    private DocumentService documentService;

    // 声明当前用户变量，供后续流程使用。
    private User currentUser;

    /**
     * 处理setup相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法在每个测试前执行。
    @BeforeEach
    void setUp() throws Exception {
        // 计算并保存当前用户结果。
        currentUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 更新field字段。
        ReflectionTestUtils.setField(documentService, "uploadPath", "./target/test-uploads");
    }

    /**
     * 处理上传文档persists文档andtriggersasyncprocessing相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentPersistsDocumentAndTriggersAsyncProcessing() throws Exception {
        // 创建文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello world".getBytes());
        // 为当前测试场景预设模拟对象行为。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 为当前测试场景预设模拟对象行为。
        when(processingService.saveFile(file, "./target/test-uploads")).thenReturn(Path.of("./target/test-uploads/saved.txt"));
        // 定义当前类型。
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            // 定义当前类型。
            Document document = invocation.getArgument(0, Document.class);
            // 根据条件判断当前分支是否执行。
            if (document.getId() == null) {
                // 更新id字段。
                document.setId("doc-1");
            }
            // 返回文档。
            return document;
        });
        // 计算并保存响应结果。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 定义当前类型。
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        // 校验依赖调用是否符合预期。
        verify(documentRepository, times(2)).save(captor.capture());
        // 断言当前结果符合测试预期。
        assertThat(response.getStatus()).isEqualTo("PARSING");
        // 断言当前结果符合测试预期。
        assertThat(response.getDocumentId()).isEqualTo("doc-1");
        // 定义当前类型。
        verify(processingService).processDocumentAsync(any(Document.class), eq(file), eq("/api/v1/documents/files/doc-1"));
        // 断言当前结果符合测试预期。
        assertThat(captor.getAllValues().get(1).getFileUrl()).isEqualTo("/api/v1/documents/files/doc-1");
    }

    /**
     * 处理上传文档returnsfailed状态whenextensionisunsupported相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentReturnsFailedStatusWhenExtensionIsUnsupported() throws Exception {
        // 创建文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "sample.exe", "application/octet-stream", "hello".getBytes());
        // 计算并保存响应结果。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 断言当前结果符合测试预期。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 断言当前结果符合测试预期。
        assertThat(response.getMessage()).contains("Unsupported file type");
        // 定义当前类型。
        verify(documentRepository, never()).save(any(Document.class));
    }

    /**
     * 处理get文档状态throwswhenowned文档doesnotexist相关逻辑。
     * @throws EntityNotFoundException 目标实体不存在时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getDocumentStatusThrowsWhenOwnedDocumentDoesNotExist() throws EntityNotFoundException {
        // 为当前测试场景预设模拟对象行为。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 为当前测试场景预设模拟对象行为。
        when(documentRepository.findByIdAndOwnerId("missing", "user-1")).thenReturn(Optional.empty());
        // 定义当前类型。
        assertThatThrownBy(() -> documentService.getDocumentStatus("missing")).isInstanceOf(EntityNotFoundException.class).hasMessage("Document not found: missing");
    }

    /**
     * 处理list文档mapsrepositoryentitiesto状态responses相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listDocumentsMapsRepositoryEntitiesToStatusResponses() throws Exception {
        // 计算并保存completed文档结果。
        Document completedDocument = TestDataFactory.document("doc-1", currentUser, "COMPLETED");
        // 更新updatedat字段。
        completedDocument.setUpdatedAt(LocalDateTime.of(2026, 4, 16, 12, 30));
        // 为当前测试场景预设模拟对象行为。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 为当前测试场景预设模拟对象行为。
        when(documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(completedDocument));
        // 计算并保存responses结果。
        List<DocumentStatusResponse> responses = documentService.listDocuments();
        // 断言当前结果符合测试预期。
        assertThat(responses).hasSize(1);
        // 断言当前结果符合测试预期。
        assertThat(responses.get(0).getDocumentId()).isEqualTo("doc-1");
        // 断言当前结果符合测试预期。
        assertThat(responses.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    /**
     * 处理上传文档returnsfailed状态whensaving文件throwsioexception相关逻辑。
     * @throws IOException 文件读写失败时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentReturnsFailedStatusWhenSavingFileThrowsIOException() throws IOException {
        // 创建文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());
        // 为当前测试场景预设模拟对象行为。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 为当前测试场景预设模拟对象行为。
        when(processingService.saveFile(file, "./target/test-uploads")).thenThrow(new IOException("disk full"));
        // 计算并保存响应结果。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 断言当前结果符合测试预期。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 断言当前结果符合测试预期。
        assertThat(response.getMessage()).contains("File save failed");
    }
}
