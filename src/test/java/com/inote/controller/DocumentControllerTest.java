// 声明当前源文件的包。
package com.inote.controller;

import com.inote.model.dto.DocumentStatusResponse;
import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.repository.UserRepository;
import com.inote.security.UnauthorizedException;
import com.inote.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 应用当前注解。
@WebMvcTest(DocumentController.class)
// 应用当前注解。
@AutoConfigureMockMvc(addFilters = false)
// 声明当前类型。
class DocumentControllerTest {

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private MockMvc mockMvc;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private DocumentController documentController;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private DocumentService documentService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private UserRepository userRepository;

    // 应用当前注解。
    @TempDir
    // 声明当前字段。
    Path tempDir;

    // 声明当前字段。
    private Path uploadRoot;
    // 声明当前字段。
    private Path uploadedFile;
    // 声明当前字段。
    private Path externalFile;

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
        uploadRoot = tempDir.resolve("uploads");
        // 执行当前语句。
        Files.createDirectories(uploadRoot);
        // 执行当前语句。
        uploadedFile = uploadRoot.resolve("uploaded-sample.txt");
        // 执行当前语句。
        externalFile = tempDir.resolve("external-sample.txt");

        // 处理当前代码结构。
        Files.writeString(uploadedFile, new ClassPathResource("test-files/uploaded-sample.txt")
                // 执行当前语句。
                .getContentAsString(StandardCharsets.UTF_8));
        // 处理当前代码结构。
        Files.writeString(externalFile, new ClassPathResource("test-files/external-sample.txt")
                // 执行当前语句。
                .getContentAsString(StandardCharsets.UTF_8));

        // 执行当前语句。
        ReflectionTestUtils.setField(documentController, "uploadPath", uploadRoot.toString());
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentReturnsOkWhenServiceAcceptsFile` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentReturnsOkWhenServiceAcceptsFile() throws Exception {
        // 处理当前代码结构。
        MockMultipartFile file = new MockMultipartFile(
                // 处理当前代码结构。
                "file",
                // 处理当前代码结构。
                "uploaded-sample.txt",
                // 处理当前代码结构。
                MediaType.TEXT_PLAIN_VALUE,
                // 执行当前语句。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 处理当前代码结构。
        when(documentService.uploadDocument(any())).thenReturn(DocumentUploadResponse.builder()
                // 处理当前代码结构。
                .documentId("doc-1")
                // 处理当前代码结构。
                .fileName("uploaded-sample.txt")
                // 处理当前代码结构。
                .status("PARSING")
                // 处理当前代码结构。
                .message("Upload accepted. Document parsing has started.")
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.status").value("PARSING"));
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentReturnsBadRequestWhenServiceMarksUploadAsFailed` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentReturnsBadRequestWhenServiceMarksUploadAsFailed() throws Exception {
        // 处理当前代码结构。
        MockMultipartFile file = new MockMultipartFile(
                // 处理当前代码结构。
                "file",
                // 处理当前代码结构。
                "uploaded-sample.txt",
                // 处理当前代码结构。
                MediaType.TEXT_PLAIN_VALUE,
                // 执行当前语句。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 处理当前代码结构。
        when(documentService.uploadDocument(any())).thenReturn(DocumentUploadResponse.builder()
                // 处理当前代码结构。
                .status("FAILED")
                // 处理当前代码结构。
                .message("Unsupported file type")
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.status").value("FAILED"))
                // 执行当前语句。
                .andExpect(jsonPath("$.message").value("Unsupported file type"));
    // 结束当前代码块。
    }

    /**
     * 描述 `uploadDocumentReturnsPayloadTooLargeWhenMultipartLimitIsExceeded` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void uploadDocumentReturnsPayloadTooLargeWhenMultipartLimitIsExceeded() throws Exception {
        // 处理当前代码结构。
        MockMultipartFile file = new MockMultipartFile(
                // 处理当前代码结构。
                "file",
                // 处理当前代码结构。
                "uploaded-sample.txt",
                // 处理当前代码结构。
                MediaType.TEXT_PLAIN_VALUE,
                // 执行当前语句。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 处理当前代码结构。
        when(documentService.uploadDocument(any()))
                // 执行当前语句。
                .thenThrow(new org.springframework.web.multipart.MaxUploadSizeExceededException(1024));

        // 处理当前代码结构。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 处理当前代码结构。
                .andExpect(status().isPayloadTooLarge())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Uploaded file exceeds the configured size limit."));
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocumentsReturnsAllDocuments` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listDocumentsReturnsAllDocuments() throws Exception {
        // 处理当前代码结构。
        when(documentService.listDocuments()).thenReturn(List.of(DocumentStatusResponse.builder()
                // 处理当前代码结构。
                .documentId("doc-1")
                // 处理当前代码结构。
                .fileName("uploaded-sample.txt")
                // 处理当前代码结构。
                .fileSize(12L)
                // 处理当前代码结构。
                .status("COMPLETED")
                // 处理当前代码结构。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                // 执行当前语句。
                .build()));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$[0].documentId").value("doc-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocumentsReturnsUnauthorizedWhenUserIsMissing` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listDocumentsReturnsUnauthorizedWhenUserIsMissing() throws Exception {
        // 执行当前语句。
        when(documentService.listDocuments()).thenThrow(new UnauthorizedException("Authentication required."));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents"))
                // 处理当前代码结构。
                .andExpect(status().isUnauthorized())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocumentsReturnsServerErrorWhenUnexpectedFailureHappens` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void listDocumentsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 执行当前语句。
        when(documentService.listDocuments()).thenThrow(new RuntimeException("boom"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents"))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatusReturnsDocumentDetails` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getDocumentStatusReturnsDocumentDetails() throws Exception {
        // 处理当前代码结构。
        when(documentService.getDocumentStatus("doc-1")).thenReturn(DocumentStatusResponse.builder()
                // 处理当前代码结构。
                .documentId("doc-1")
                // 处理当前代码结构。
                .fileName("uploaded-sample.txt")
                // 处理当前代码结构。
                .fileSize(12L)
                // 处理当前代码结构。
                .status("PARSING")
                // 处理当前代码结构。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/doc-1"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.status").value("PARSING"));
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatusReturnsNotFoundWhenDocumentDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getDocumentStatusReturnsNotFoundWhenDocumentDoesNotExist() throws Exception {
        // 处理当前代码结构。
        when(documentService.getDocumentStatus("missing"))
                // 执行当前语句。
                .thenThrow(new EntityNotFoundException("Document not found: missing"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/missing"))
                // 处理当前代码结构。
                .andExpect(status().isNotFound())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Document not found: missing"));
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatusReturnsServerErrorWhenUnexpectedFailureHappens` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getDocumentStatusReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 执行当前语句。
        when(documentService.getDocumentStatus("doc-1")).thenThrow(new RuntimeException("boom"));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/doc-1"))
                // 处理当前代码结构。
                .andExpect(status().isInternalServerError())
                // 执行当前语句。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    // 结束当前代码块。
    }

    /**
     * 描述 `getFileReturnsResourceWhenFileExistsInsideUploadRoot` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getFileReturnsResourceWhenFileExistsInsideUploadRoot() throws Exception {
        // 处理当前代码结构。
        when(documentService.getOwnedDocument("doc-1")).thenReturn(Document.builder()
                // 处理当前代码结构。
                .id("doc-1")
                // 处理当前代码结构。
                .fileName("uploaded-sample.txt")
                // 处理当前代码结构。
                .filePath(uploadedFile.toString())
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/files/doc-1"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 执行当前语句。
                .andExpect(header().string("Content-Disposition", "inline; filename=\"uploaded-sample.txt\""));
    // 结束当前代码块。
    }

    /**
     * 描述 `getFileReturnsBadRequestWhenFileIsOutsideUploadRoot` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getFileReturnsBadRequestWhenFileIsOutsideUploadRoot() throws Exception {
        // 处理当前代码结构。
        when(documentService.getOwnedDocument("doc-2")).thenReturn(Document.builder()
                // 处理当前代码结构。
                .id("doc-2")
                // 处理当前代码结构。
                .fileName("external-sample.txt")
                // 处理当前代码结构。
                .filePath(externalFile.toString())
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/files/doc-2"))
                // 执行当前语句。
                .andExpect(status().isBadRequest());
    // 结束当前代码块。
    }

    /**
     * 描述 `getFileReturnsNotFoundWhenFileDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void getFileReturnsNotFoundWhenFileDoesNotExist() throws Exception {
        // 处理当前代码结构。
        when(documentService.getOwnedDocument("doc-3")).thenReturn(Document.builder()
                // 处理当前代码结构。
                .id("doc-3")
                // 处理当前代码结构。
                .fileName("missing.txt")
                // 处理当前代码结构。
                .filePath(uploadRoot.resolve("missing.txt").toString())
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/documents/files/doc-3"))
                // 执行当前语句。
                .andExpect(status().isNotFound());
    // 结束当前代码块。
    }
// 结束当前代码块。
}
