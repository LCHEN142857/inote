// 声明测试类所在包。
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

// 标记当前类使用 Mockito 扩展。
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    // 声明文档仓储模拟对象。
    @Mock
    private DocumentRepository documentRepository;

    // 声明文档处理服务模拟对象。
    @Mock
    private DocumentProcessingService processingService;

    // 声明当前用户服务模拟对象。
    @Mock
    private CurrentUserService currentUserService;

    // 声明被测服务实例。
    @InjectMocks
    private DocumentService documentService;

    // 声明测试用户实体。
    private User currentUser;

    /**
     * 初始化文档服务测试环境。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当反射写入字段失败时抛出异常。
     */
    @BeforeEach
    void setUp() throws Exception {
        // 构造测试用户实体。
        currentUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 写入测试上传目录配置。
        ReflectionTestUtils.setField(documentService, "uploadPath", "./target/test-uploads");
    }

    /**
     * 验证合法文件上传会保存文档并触发异步处理。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当模拟文件保存抛出异常时向外抛出。
     */
    @Test
    void uploadDocumentPersistsDocumentAndTriggersAsyncProcessing() throws Exception {
        // 构造测试文件对象。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello world".getBytes());
        // 模拟当前用户查询结果。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 模拟文件保存路径。
        when(processingService.saveFile(file, "./target/test-uploads")).thenReturn(Path.of("./target/test-uploads/saved.txt"));
        // 模拟仓储保存行为并在首次保存时补齐主键。
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            // 提取当前保存的文档实体。
            Document document = invocation.getArgument(0, Document.class);
            // 为首次保存补齐主键。
            if (document.getId() == null) {
                // 设置主键以模拟持久化结果。
                document.setId("doc-1");
            }
            // 返回持久化后的实体。
            return document;
        });
        // 调用上传方法。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 捕获最后一次保存的文档实体。
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        // 断言保存行为至少发生一次。
        verify(documentRepository, times(2)).save(captor.capture());
        // 断言响应状态为处理中。
        assertThat(response.getStatus()).isEqualTo("PARSING");
        // 断言响应中的文档主键已返回。
        assertThat(response.getDocumentId()).isEqualTo("doc-1");
        // 断言异步处理被触发。
        verify(processingService).processDocumentAsync(any(Document.class), eq(file), eq("/api/v1/documents/files/doc-1"));
        // 断言最终保存的文件地址已生成。
        assertThat(captor.getAllValues().get(1).getFileUrl()).isEqualTo("/api/v1/documents/files/doc-1");
    }

    /**
     * 验证非法扩展名文件会直接返回失败响应。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void uploadDocumentReturnsFailedStatusWhenExtensionIsUnsupported() throws Exception {
        // 构造非法扩展名文件。
        MockMultipartFile file = new MockMultipartFile("file", "sample.exe", "application/octet-stream", "hello".getBytes());
        // 调用上传方法。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 断言状态为失败。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 断言提示包含不支持的文件类型。
        assertThat(response.getMessage()).contains("Unsupported file type");
        // 断言非法请求不会写库。
        verify(documentRepository, never()).save(any(Document.class));
    }

    /**
     * 验证查询不存在的用户文档会抛出未找到异常。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws EntityNotFoundException 当文档不存在时抛出。
     */
    @Test
    void getDocumentStatusThrowsWhenOwnedDocumentDoesNotExist() throws EntityNotFoundException {
        // 模拟当前用户查询结果。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 模拟按用户查询文档返回空结果。
        when(documentRepository.findByIdAndOwnerId("missing", "user-1")).thenReturn(Optional.empty());
        // 断言查询状态会抛出未找到异常。
        assertThatThrownBy(() -> documentService.getDocumentStatus("missing")).isInstanceOf(EntityNotFoundException.class).hasMessage("Document not found: missing");
    }

    /**
     * 验证列出文档时只返回当前用户的文档摘要。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void listDocumentsMapsRepositoryEntitiesToStatusResponses() throws Exception {
        // 构造已完成文档实体。
        Document completedDocument = TestDataFactory.document("doc-1", currentUser, "COMPLETED");
        // 设置文档更新时间以便断言映射。
        completedDocument.setUpdatedAt(LocalDateTime.of(2026, 4, 16, 12, 30));
        // 模拟当前用户查询结果。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 模拟文档仓储返回当前用户文档列表。
        when(documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(completedDocument));
        // 调用列表方法。
        List<DocumentStatusResponse> responses = documentService.listDocuments();
        // 断言只返回一条文档记录。
        assertThat(responses).hasSize(1);
        // 断言返回文档主键正确。
        assertThat(responses.get(0).getDocumentId()).isEqualTo("doc-1");
        // 断言返回状态正确。
        assertThat(responses.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    /**
     * 验证文件保存失败时会返回失败响应。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws IOException 当文件保存异常被模拟时由服务内部处理。
     */
    @Test
    void uploadDocumentReturnsFailedStatusWhenSavingFileThrowsIOException() throws IOException {
        // 构造合法测试文件。
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());
        // 模拟当前用户查询结果。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 模拟文件保存抛出异常。
        when(processingService.saveFile(file, "./target/test-uploads")).thenThrow(new IOException("disk full"));
        // 调用上传方法。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 断言上传结果为失败。
        assertThat(response.getStatus()).isEqualTo("FAILED");
        // 断言响应信息包含文件保存失败原因。
        assertThat(response.getMessage()).contains("File save failed");
    }
}
