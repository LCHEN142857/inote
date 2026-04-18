// 声明当前源文件所属包。
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

// 应用 `WebMvcTest` 注解声明当前行为。
@WebMvcTest(DocumentController.class)
// 在测试环境中注入 MockMvc。
@AutoConfigureMockMvc(addFilters = false)
// 定义 `DocumentControllerTest` 类型。
class DocumentControllerTest {

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明mockmvc变量，供后续流程使用。
    private MockMvc mockMvc;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明文档controller变量，供后续流程使用。
    private DocumentController documentController;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明文档service变量，供后续流程使用。
    private DocumentService documentService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    // 为当前测试注入临时目录。
    @TempDir
    // 声明tempdir变量，供后续流程使用。
    Path tempDir;

    // 声明上传root变量，供后续流程使用。
    private Path uploadRoot;
    // 声明uploaded文件变量，供后续流程使用。
    private Path uploadedFile;
    // 声明external文件变量，供后续流程使用。
    private Path externalFile;

    /**
     * 处理setup相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法在每个测试前执行。
    @BeforeEach
    void setUp() throws Exception {
        // 计算并保存上传root结果。
        uploadRoot = tempDir.resolve("uploads");
        // 调用 `createDirectories` 完成当前步骤。
        Files.createDirectories(uploadRoot);
        // 计算并保存uploaded文件结果。
        uploadedFile = uploadRoot.resolve("uploaded-sample.txt");
        // 计算并保存external文件结果。
        externalFile = tempDir.resolve("external-sample.txt");

        // 围绕文件writeuploaded补充当前业务语句。
        Files.writeString(uploadedFile, new ClassPathResource("test-files/uploaded-sample.txt")
                // 设置get内容asstring字段的取值。
                .getContentAsString(StandardCharsets.UTF_8));
        // 围绕文件writeexternal补充当前业务语句。
        Files.writeString(externalFile, new ClassPathResource("test-files/external-sample.txt")
                // 设置get内容asstring字段的取值。
                .getContentAsString(StandardCharsets.UTF_8));

        // 更新field字段。
        ReflectionTestUtils.setField(documentController, "uploadPath", uploadRoot.toString());
    }

    /**
     * 处理上传文档returnsokwhenserviceaccepts文件相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentReturnsOkWhenServiceAcceptsFile() throws Exception {
        // 围绕mockmultipart文件补充当前业务语句。
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                // 围绕mediatypetext补充当前业务语句。
                MediaType.TEXT_PLAIN_VALUE,
                // 调用 `ClassPathResource` 完成当前步骤。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 围绕when文档service补充当前业务语句。
        when(documentService.uploadDocument(any())).thenReturn(DocumentUploadResponse.builder()
                // 设置文档id字段的取值。
                .documentId("doc-1")
                // 设置文件name字段的取值。
                .fileName("uploaded-sample.txt")
                // 设置状态字段的取值。
                .status("PARSING")
                // 设置消息字段的取值。
                .message("Upload accepted. Document parsing has started.")
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.status").value("PARSING"));
    }

    /**
     * 处理上传文档returnsbad请求whenservicemarks上传asfailed相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentReturnsBadRequestWhenServiceMarksUploadAsFailed() throws Exception {
        // 围绕mockmultipart文件补充当前业务语句。
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                // 围绕mediatypetext补充当前业务语句。
                MediaType.TEXT_PLAIN_VALUE,
                // 调用 `ClassPathResource` 完成当前步骤。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 围绕when文档service补充当前业务语句。
        when(documentService.uploadDocument(any())).thenReturn(DocumentUploadResponse.builder()
                // 设置状态字段的取值。
                .status("FAILED")
                // 设置消息字段的取值。
                .message("Unsupported file type")
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.status").value("FAILED"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.message").value("Unsupported file type"));
    }

    /**
     * 处理上传文档returnspayloadtoolargewhenmultipartlimitisexceeded相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void uploadDocumentReturnsPayloadTooLargeWhenMultipartLimitIsExceeded() throws Exception {
        // 围绕mockmultipart文件补充当前业务语句。
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-sample.txt",
                // 围绕mediatypetext补充当前业务语句。
                MediaType.TEXT_PLAIN_VALUE,
                // 调用 `ClassPathResource` 完成当前步骤。
                new ClassPathResource("test-files/uploaded-sample.txt").getInputStream());

        // 围绕when文档service补充当前业务语句。
        when(documentService.uploadDocument(any()))
                // 设置thenthrow字段的取值。
                .thenThrow(new org.springframework.web.multipart.MaxUploadSizeExceededException(1024));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                // 继续校验接口响应结果。
                .andExpect(status().isPayloadTooLarge())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Uploaded file exceeds the configured size limit."));
    }

    /**
     * 处理list文档returnsall文档相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listDocumentsReturnsAllDocuments() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.listDocuments()).thenReturn(List.of(DocumentStatusResponse.builder()
                // 设置文档id字段的取值。
                .documentId("doc-1")
                // 设置文件name字段的取值。
                .fileName("uploaded-sample.txt")
                // 设置文件size字段的取值。
                .fileSize(12L)
                // 设置状态字段的取值。
                .status("COMPLETED")
                // 设置updatedat字段的取值。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                // 完成当前建造者对象的组装。
                .build()));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$[0].documentId").value("doc-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    /**
     * 处理list文档returnsunauthorizedwhen用户ismissing相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listDocumentsReturnsUnauthorizedWhenUserIsMissing() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(documentService.listDocuments()).thenThrow(new UnauthorizedException("Authentication required."));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents"))
                // 继续校验接口响应结果。
                .andExpect(status().isUnauthorized())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    /**
     * 处理list文档returnsserver错误信息whenunexpectedfailurehappens相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void listDocumentsReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(documentService.listDocuments()).thenThrow(new RuntimeException("boom"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents"))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    }

    /**
     * 处理get文档状态returns文档details相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getDocumentStatusReturnsDocumentDetails() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.getDocumentStatus("doc-1")).thenReturn(DocumentStatusResponse.builder()
                // 设置文档id字段的取值。
                .documentId("doc-1")
                // 设置文件name字段的取值。
                .fileName("uploaded-sample.txt")
                // 设置文件size字段的取值。
                .fileSize(12L)
                // 设置状态字段的取值。
                .status("PARSING")
                // 设置updatedat字段的取值。
                .updatedAt(LocalDateTime.of(2026, 4, 16, 12, 0))
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/doc-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.documentId").value("doc-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.status").value("PARSING"));
    }

    /**
     * 处理get文档状态returnsnotfoundwhen文档doesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getDocumentStatusReturnsNotFoundWhenDocumentDoesNotExist() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.getDocumentStatus("missing"))
                // 设置thenthrow字段的取值。
                .thenThrow(new EntityNotFoundException("Document not found: missing"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/missing"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Document not found: missing"));
    }

    /**
     * 处理get文档状态returnsserver错误信息whenunexpectedfailurehappens相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getDocumentStatusReturnsServerErrorWhenUnexpectedFailureHappens() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(documentService.getDocumentStatus("doc-1")).thenThrow(new RuntimeException("boom"));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/doc-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isInternalServerError())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.answer").value("Unexpected server error. Please try again later."));
    }

    /**
     * 处理get文件returnsresourcewhen文件existsinside上传root相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getFileReturnsResourceWhenFileExistsInsideUploadRoot() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.getOwnedDocument("doc-1")).thenReturn(Document.builder()
                // 设置id字段的取值。
                .id("doc-1")
                // 设置文件name字段的取值。
                .fileName("uploaded-sample.txt")
                // 设置文件path字段的取值。
                .filePath(uploadedFile.toString())
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/files/doc-1"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(header().string("Content-Disposition", "inline; filename=\"uploaded-sample.txt\""));
    }

    /**
     * 处理get文件returnsbad请求when文件isoutside上传root相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getFileReturnsBadRequestWhenFileIsOutsideUploadRoot() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.getOwnedDocument("doc-2")).thenReturn(Document.builder()
                // 设置id字段的取值。
                .id("doc-2")
                // 设置文件name字段的取值。
                .fileName("external-sample.txt")
                // 设置文件path字段的取值。
                .filePath(externalFile.toString())
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/files/doc-2"))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest());
    }

    /**
     * 处理get文件returnsnotfoundwhen文件doesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void getFileReturnsNotFoundWhenFileDoesNotExist() throws Exception {
        // 围绕when文档service补充当前业务语句。
        when(documentService.getOwnedDocument("doc-3")).thenReturn(Document.builder()
                // 设置id字段的取值。
                .id("doc-3")
                // 设置文件name字段的取值。
                .fileName("missing.txt")
                // 设置文件path字段的取值。
                .filePath(uploadRoot.resolve("missing.txt").toString())
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/documents/files/doc-3"))
                // 继续校验接口响应结果。
                .andExpect(status().isNotFound());
    }
}
