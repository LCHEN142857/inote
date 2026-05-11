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

// 负责当前用户的文档上传、状态查询和归属校验。
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    // 允许上传的文档扩展名集合。
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "xlsx", "xls", "txt", "csv"
    );

    // 提供文档元数据持久化能力。
    private final DocumentRepository documentRepository;
    // 负责物理文件保存和异步解析。
    private final DocumentProcessingService processingService;
    // 提供当前登录用户上下文。
    private final CurrentUserService currentUserService;

    // 配置化上传目录，默认为 ./uploads。
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 接收并登记用户上传的文档。
     * @param file 用户上传的文件。
     * @return 上传受理结果。
     * @throws IllegalArgumentException 文件为空、文件名缺失或扩展名不支持时抛出。
     * @throws DocumentStorageException 文件保存失败时抛出。
     */
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        // 先做格式和空值校验，避免无效文件进入后续流程。
        validateFile(file);
        // 只允许当前登录用户上传到自己的文档空间。
        User user = currentUserService.getCurrentUser();

        try {
            // 先将物理文件写入磁盘，再创建数据库记录。
            Path savedPath = processingService.saveFile(file, uploadPath);

            // 先创建一个解析中状态的文档记录。
            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(savedPath.toString())
                    .fileUrl("")
                    .owner(user)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("PARSING")
                    .build();

            // 先保存一次以获取文档 ID。
            document = documentRepository.save(document);
            // 生成可用于前端预览的文档访问路径。
            String fileUrl = "/api/v1/documents/files/" + document.getId();
            // 回填文件访问地址后再保存一次。
            document.setFileUrl(fileUrl);
            documentRepository.save(document);

            // 将解析工作放到异步任务中，避免上传请求阻塞。
            processingService.processDocumentAsync(document.getId(), fileUrl);

            // 返回前端可立即展示的上传受理结果。
            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(document.getStatus())
                    .message("Upload accepted. Document parsing has started.")
                    .build();
        } catch (IOException e) {
            // 文件保存失败时记录异常并转换为业务异常。
            log.error("Failed to save file", e);
            throw new DocumentStorageException("File save failed.", e);
        }
    }

    /**
     * 查询单个文档的当前状态。
     * @param documentId 文档 ID。
     * @return 文档状态响应。
     * @throws jakarta.persistence.EntityNotFoundException 文档不存在或不属于当前用户时抛出。
     */
    public DocumentStatusResponse getDocumentStatus(String documentId) {
        // 归属校验在 findDocument 内完成。
        return toStatusResponse(findDocument(documentId));
    }

    /**
     * 列出当前用户拥有的所有文档状态。
     * @return 当前用户文档状态列表。
     * @throws com.inote.security.UnauthorizedException 当前请求未认证时抛出。
     */
    public List<DocumentStatusResponse> listDocuments() {
        // 只查询当前用户的数据，避免越权泄露。
        User user = currentUserService.getCurrentUser();
        return documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(this::toStatusResponse)
                .toList();
    }

    /**
     * 返回当前用户拥有的文档实体。
     * @param documentId 文档 ID。
     * @return 文档实体。
     * @throws jakarta.persistence.EntityNotFoundException 文档不存在或不属于当前用户时抛出。
     */
    public Document getOwnedDocument(String documentId) {
        return findDocument(documentId);
    }

    /**
     * 按当前用户和文档 ID 查找文档。
     * @param documentId 文档 ID。
     * @return 受当前用户拥有的文档实体。
     * @throws jakarta.persistence.EntityNotFoundException 文档不存在或不属于当前用户时抛出。
     */
    private Document findDocument(String documentId) {
        // 将用户身份纳入查询条件，确保只能访问自己的文档。
        User user = currentUserService.getCurrentUser();
        return documentRepository.findByIdAndOwnerId(documentId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    /**
     * 将文档实体映射为状态响应。
     * @param document 文档实体。
     * @return 文档状态响应。
     */
    private DocumentStatusResponse toStatusResponse(Document document) {
        // 仅返回状态相关字段，不暴露内部存储路径。
        return DocumentStatusResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * 校验上传文件的存在性和扩展名。
     * @param file 用户上传的文件。
     * @throws IllegalArgumentException 文件为空、文件名缺失或扩展名不支持时抛出。
     */
    private void validateFile(MultipartFile file) {
        // 空文件直接拒绝，避免后续出现空指针或无效存储记录。
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        // 原始文件名缺失时无法判断格式。
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name must not be empty.");
        }

        // 通过扩展名控制允许的文档类型。
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension
                    + ". Supported types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * 提取文件扩展名。
     * @param filename 原始文件名。
     * @return 扩展名，缺失时返回空串。
     */
    private String getFileExtension(String filename) {
        // 没有扩展名或以点结尾时按空扩展名处理。
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
