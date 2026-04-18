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

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentController documentController;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    private Path uploadRoot;
    private Path uploadedFile;
    private Path externalFile;

    @BeforeEach
    void setUp() throws Exception {
        uploadRoot = tempDir.resolve("uploads");
        Files.createDirectories(uploadRoot);
        uploadedFile = uploadRoot.resolve("uploaded-sample.txt");
        externalFile = tempDir.resolve("external-sample.txt");

        Files.writeString(uploadedFile,
                new ClassPathResource("test-files/uploaded-sample.txt").getContentAsString(StandardCharsets.UTF_8));
        Files.writeString(externalFile,
                new ClassPathResource("test-files/external-sample.txt").getContentAsString(StandardCharsets.UTF_8));

        ReflectionTestUtils.setField(documentController, "uploadPath", uploadRoot.toString());
    }

    @Test
    void uploadDocumentReturnsOkWhenServiceAcceptsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        when(documentService.uploadDocument(any())).thenReturn(DocumentUploadResponse.builder()
                .documentId("doc-1")
                .fileName("uploaded-sample.txt")
                .status("PARSING")
                .message("Upload accepted. Document parsing has started.")
                .build());

        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                .andExpect(jsonPath("$.status").value("PARSING"));
    }

    @Test
    void uploadDocumentReturnsBadRequestWhenValidationFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        when(documentService.uploadDocument(any())).thenThrow(new IllegalArgumentException("Unsupported file type"));

        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported file type"));
    }

    @Test
    void uploadDocumentReturnsPayloadTooLargeWhenMultipartLimitIsExceeded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        when(documentService.uploadDocument(any()))
                .thenThrow(new org.springframework.web.multipart.MaxUploadSizeExceededException(1024));

        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("Uploaded file exceeds the configured size limit."));
    }

    @Test
    void listDocumentsReturnsAllDocuments() throws Exception {
        when(documentService.listDocuments()).thenReturn(List.of(DocumentStatusResponse.builder()
                .documentId("doc-1")
                .fileName("uploaded-sample.txt")
                .fileSize(12L)
                .status("COMPLETED")
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                .build()));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value("doc-1"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void listDocumentsReturnsUnauthorizedWhenUserIsMissing() throws Exception {
        when(documentService.listDocuments()).thenThrow(new UnauthorizedException("Authentication required."));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    @Test
    void listDocumentsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(documentService.listDocuments()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    @Test
    void getDocumentStatusReturnsDocumentDetails() throws Exception {
        when(documentService.getDocumentStatus("doc-1")).thenReturn(DocumentStatusResponse.builder()
                .documentId("doc-1")
                .fileName("uploaded-sample.txt")
                .fileSize(12L)
                .status("PARSING")
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                .build());

        mockMvc.perform(get("/api/v1/documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                .andExpect(jsonPath("$.status").value("PARSING"));
    }

    @Test
    void getDocumentStatusReturnsNotFoundWhenDocumentDoesNotExist() throws Exception {
        when(documentService.getDocumentStatus("missing"))
                .thenThrow(new EntityNotFoundException("Document not found: missing"));

        mockMvc.perform(get("/api/v1/documents/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found: missing"));
    }

    @Test
    void getDocumentStatusReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        when(documentService.getDocumentStatus("doc-1")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/documents/doc-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected server error. Please try again later."));
    }

    @Test
    void getFileReturnsResourceWhenFileExistsInsideUploadRoot() throws Exception {
        when(documentService.getOwnedDocument("doc-1")).thenReturn(Document.builder()
                .id("doc-1")
                .fileName("uploaded-sample.txt")
                .filePath(uploadedFile.toString())
                .build());

        mockMvc.perform(get("/api/v1/documents/files/doc-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"uploaded-sample.txt\""));
    }

    @Test
    void getFileReturnsBadRequestWhenFileIsOutsideUploadRoot() throws Exception {
        when(documentService.getOwnedDocument("doc-2")).thenReturn(Document.builder()
                .id("doc-2")
                .fileName("external-sample.txt")
                .filePath(externalFile.toString())
                .build());

        mockMvc.perform(get("/api/v1/documents/files/doc-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFileReturnsNotFoundWhenFileDoesNotExist() throws Exception {
        when(documentService.getOwnedDocument("doc-3")).thenReturn(Document.builder()
                .id("doc-3")
                .fileName("missing.txt")
                .filePath(uploadRoot.resolve("missing.txt").toString())
                .build());

        mockMvc.perform(get("/api/v1/documents/files/doc-3"))
                .andExpect(status().isNotFound());
    }
}
