// 声明当前源文件所属包。
package com.inote.service;

import com.inote.model.dto.DocumentStatusResponse;
import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.model.entity.User;
import com.inote.repository.DocumentRepository;
import com.inote.security.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义文档服务，负责校验上传文件并维护文档记录。
public class DocumentService {

    // 定义allowedextensions常量。
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "txt", "csv"
    );

    // 声明文档repository变量，供后续流程使用。
    private final DocumentRepository documentRepository;
    // 声明processingservice变量，供后续流程使用。
    private final DocumentProcessingService processingService;
    // 声明当前用户service变量，供后续流程使用。
    private final CurrentUserService currentUserService;

    // 从配置文件中注入当前字段的取值。
    @Value("${file.upload.path:./uploads}")
    // 声明上传path变量，供后续流程使用。
    private String uploadPath;

    /**
     * 校验上传文件、创建文档记录并异步启动解析流程。
     * @param file 文件参数。
     * @return 文档上传响应结果。
     */
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 调用 `validateFile` 完成当前步骤。
            validateFile(file);
            // 获取当前登录用户。
            User user = currentUserService.getCurrentUser();

            // 计算并保存savedpath结果。
            Path savedPath = processingService.saveFile(file, uploadPath);

            // 围绕文档文档文档补充当前业务语句。
            Document document = Document.builder()
                    // 设置文件name字段的取值。
                    .fileName(file.getOriginalFilename())
                    // 设置文件path字段的取值。
                    .filePath(savedPath.toString())
                    // 设置文件url字段的取值。
                    .fileUrl("")
                    // 设置所属用户字段的取值。
                    .owner(user)
                    // 设置内容type字段的取值。
                    .contentType(file.getContentType())
                    // 设置文件size字段的取值。
                    .fileSize(file.getSize())
                    // 设置状态字段的取值。
                    .status("PARSING")
                    // 完成当前建造者对象的组装。
                    .build();

            // 保存文档对象。
            document = documentRepository.save(document);
            // 计算并保存文件url结果。
            String fileUrl = "/api/v1/documents/files/" + document.getId();
            // 更新文件url字段。
            document.setFileUrl(fileUrl);
            // 保存当前对象到持久化层。
            documentRepository.save(document);
            // 调用 `processDocumentAsync` 完成当前步骤。
            processingService.processDocumentAsync(document, file, fileUrl);

            // 返回组装完成的结果对象。
            return DocumentUploadResponse.builder()
                    // 设置文档id字段的取值。
                    .documentId(document.getId())
                    // 设置文件name字段的取值。
                    .fileName(document.getFileName())
                    // 设置状态字段的取值。
                    .status(document.getStatus())
                    // 设置消息字段的取值。
                    .message("Upload accepted. Document parsing has started.")
                    // 完成当前建造者对象的组装。
                    .build();
        } catch (IllegalArgumentException e) {
            // 记录当前流程的运行日志。
            log.warn("File validation failed: {}", e.getMessage());
            // 返回组装完成的结果对象。
            return DocumentUploadResponse.builder()
                    // 设置状态字段的取值。
                    .status("FAILED")
                    // 设置消息字段的取值。
                    .message(e.getMessage())
                    // 完成当前建造者对象的组装。
                    .build();
        } catch (IOException e) {
            // 记录当前流程的运行日志。
            log.error("Failed to save file", e);
            // 返回组装完成的结果对象。
            return DocumentUploadResponse.builder()
                    // 设置状态字段的取值。
                    .status("FAILED")
                    // 设置消息字段的取值。
                    .message("File save failed: " + e.getMessage())
                    // 完成当前建造者对象的组装。
                    .build();
        }
    }

    /**
     * 查询指定文档的最新处理状态。
     * @param documentId 文档id参数。
     * @return 文档状态响应结果。
     */
    public DocumentStatusResponse getDocumentStatus(String documentId) {
        // 返回 `toStatusResponse` 的处理结果。
        return toStatusResponse(findDocument(documentId));
    }

    /**
     * 查询当前用户名下的全部文档状态。
     * @return 列表形式的处理结果。
     */
    public List<DocumentStatusResponse> listDocuments() {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 返回 `findAllByOwnerIdOrderByUpdatedAtDesc` 的处理结果。
        return documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream()
                // 设置map字段的取值。
                .map(this::toStatusResponse)
                // 设置tolist字段的取值。
                .toList();
    }

    /**
     * 读取当前用户拥有的文档实体。
     * @param documentId 文档id参数。
     * @return 匹配到的文档实体。
     */
    public Document getOwnedDocument(String documentId) {
        // 返回 `findDocument` 的处理结果。
        return findDocument(documentId);
    }

    /**
     * 处理find文档相关逻辑。
     * @param documentId 文档id参数。
     * @return 匹配到的文档实体。
     */
    private Document findDocument(String documentId) {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 返回 `findByIdAndOwnerId` 的处理结果。
        return documentRepository.findByIdAndOwnerId(documentId, user.getId())
                // 设置orelsethrow字段的取值。
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    /**
     * 处理to状态响应相关逻辑。
     * @param document 文档参数。
     * @return 文档状态响应结果。
     */
    private DocumentStatusResponse toStatusResponse(Document document) {
        // 返回组装完成的结果对象。
        return DocumentStatusResponse.builder()
                // 设置文档id字段的取值。
                .documentId(document.getId())
                // 设置文件name字段的取值。
                .fileName(document.getFileName())
                // 设置文件size字段的取值。
                .fileSize(document.getFileSize())
                // 设置状态字段的取值。
                .status(document.getStatus())
                // 设置错误信息消息字段的取值。
                .errorMessage(document.getErrorMessage())
                // 设置updatedat字段的取值。
                .updatedAt(document.getUpdatedAt())
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 处理validate文件相关逻辑。
     * @param file 文件参数。
     */
    private void validateFile(MultipartFile file) {
        // 根据条件判断当前分支是否执行。
        if (file == null || file.isEmpty()) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("File must not be empty.");
        }

        // 计算并保存originalfilename结果。
        String originalFilename = file.getOriginalFilename();
        // 根据条件判断当前分支是否执行。
        if (originalFilename == null) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("File name must not be empty.");
        }

        // 计算并保存extension结果。
        String extension = getFileExtension(originalFilename).toLowerCase();
        // 根据条件判断当前分支是否执行。
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Unsupported file type: " + extension +
                    ". Supported types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * 处理get文件extension相关逻辑。
     * @param filename filename参数。
     * @return 处理后的字符串结果。
     */
    private String getFileExtension(String filename) {
        // 计算并保存lastdotindex结果。
        int lastDotIndex = filename.lastIndexOf('.');
        // 根据条件判断当前分支是否执行。
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回""。
            return "";
        }
        // 返回 `substring` 的处理结果。
        return filename.substring(lastDotIndex + 1);
    }
}
