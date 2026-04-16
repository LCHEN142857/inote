// 声明当前源文件的包。
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

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class DocumentService {

    /**
     * 描述 `Set.of` 操作。
     *
     * @param "pdf" 输入参数 `"pdf"`。
     * @param "docx" 输入参数 `"docx"`。
     * @param "doc" 输入参数 `"doc"`。
     * @param "xlsx" 输入参数 `"xlsx"`。
     * @param "xls" 输入参数 `"xls"`。
     * @param "txt" 输入参数 `"txt"`。
     * @param "csv" 输入参数 `"csv"`。
     * @return 类型为 `Set<String> ALLOWED_EXTENSIONS =` 的返回值。
     */
    // 处理当前代码结构。
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 处理当前代码结构。
            "pdf", "docx", "doc", "xlsx", "xls", "txt", "csv"
    // 声明当前字段。
    );

    // 声明当前字段。
    private final DocumentRepository documentRepository;
    // 声明当前字段。
    private final DocumentProcessingService processingService;
    // 声明当前字段。
    private final CurrentUserService currentUserService;

    // 应用当前注解。
    @Value("${file.upload.path:./uploads}")
    // 声明当前字段。
    private String uploadPath;

    /**
     * 描述 `uploadDocument` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `DocumentUploadResponse` 的返回值。
     */
    // 处理当前代码结构。
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            validateFile(file);
            // 执行当前语句。
            User user = currentUserService.getCurrentUser();

            // 执行当前语句。
            Path savedPath = processingService.saveFile(file, uploadPath);

            // 处理当前代码结构。
            Document document = Document.builder()
                    // 处理当前代码结构。
                    .fileName(file.getOriginalFilename())
                    // 处理当前代码结构。
                    .filePath(savedPath.toString())
                    // 处理当前代码结构。
                    .fileUrl("")
                    // 处理当前代码结构。
                    .owner(user)
                    // 处理当前代码结构。
                    .contentType(file.getContentType())
                    // 处理当前代码结构。
                    .fileSize(file.getSize())
                    // 处理当前代码结构。
                    .status("PARSING")
                    // 执行当前语句。
                    .build();

            // 执行当前语句。
            document = documentRepository.save(document);
            // 执行当前语句。
            String fileUrl = "/api/v1/documents/files/" + document.getId();
            // 执行当前语句。
            document.setFileUrl(fileUrl);
            // 执行当前语句。
            documentRepository.save(document);
            // 执行当前语句。
            processingService.processDocumentAsync(document, file, fileUrl);

            // 返回当前结果。
            return DocumentUploadResponse.builder()
                    // 处理当前代码结构。
                    .documentId(document.getId())
                    // 处理当前代码结构。
                    .fileName(document.getFileName())
                    // 处理当前代码结构。
                    .status(document.getStatus())
                    // 处理当前代码结构。
                    .message("Upload accepted. Document parsing has started.")
                    // 执行当前语句。
                    .build();
        // 处理当前代码结构。
        } catch (IllegalArgumentException e) {
            // 执行当前语句。
            log.warn("File validation failed: {}", e.getMessage());
            // 返回当前结果。
            return DocumentUploadResponse.builder()
                    // 处理当前代码结构。
                    .status("FAILED")
                    // 处理当前代码结构。
                    .message(e.getMessage())
                    // 执行当前语句。
                    .build();
        // 处理当前代码结构。
        } catch (IOException e) {
            // 执行当前语句。
            log.error("Failed to save file", e);
            // 返回当前结果。
            return DocumentUploadResponse.builder()
                    // 处理当前代码结构。
                    .status("FAILED")
                    // 处理当前代码结构。
                    .message("File save failed: " + e.getMessage())
                    // 执行当前语句。
                    .build();
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatus` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @return 类型为 `DocumentStatusResponse` 的返回值。
     */
    // 处理当前代码结构。
    public DocumentStatusResponse getDocumentStatus(String documentId) {
        // 返回当前结果。
        return toStatusResponse(findDocument(documentId));
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocuments` 操作。
     *
     * @return 类型为 `List<DocumentStatusResponse>` 的返回值。
     */
    // 处理当前代码结构。
    public List<DocumentStatusResponse> listDocuments() {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 返回当前结果。
        return documentRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.getId()).stream()
                // 处理当前代码结构。
                .map(this::toStatusResponse)
                // 执行当前语句。
                .toList();
    // 结束当前代码块。
    }

    /**
     * 描述 `getOwnedDocument` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @return 类型为 `Document` 的返回值。
     */
    // 处理当前代码结构。
    public Document getOwnedDocument(String documentId) {
        // 返回当前结果。
        return findDocument(documentId);
    // 结束当前代码块。
    }

    /**
     * 描述 `findDocument` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @return 类型为 `Document` 的返回值。
     */
    // 处理当前代码结构。
    private Document findDocument(String documentId) {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 返回当前结果。
        return documentRepository.findByIdAndOwnerId(documentId, user.getId())
                // 执行当前语句。
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    // 结束当前代码块。
    }

    /**
     * 描述 `toStatusResponse` 操作。
     *
     * @param document 输入参数 `document`。
     * @return 类型为 `DocumentStatusResponse` 的返回值。
     */
    // 处理当前代码结构。
    private DocumentStatusResponse toStatusResponse(Document document) {
        // 返回当前结果。
        return DocumentStatusResponse.builder()
                // 处理当前代码结构。
                .documentId(document.getId())
                // 处理当前代码结构。
                .fileName(document.getFileName())
                // 处理当前代码结构。
                .fileSize(document.getFileSize())
                // 处理当前代码结构。
                .status(document.getStatus())
                // 处理当前代码结构。
                .errorMessage(document.getErrorMessage())
                // 处理当前代码结构。
                .updatedAt(document.getUpdatedAt())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `validateFile` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void validateFile(MultipartFile file) {
        // 执行当前流程控制分支。
        if (file == null || file.isEmpty()) {
            // 抛出当前异常。
            throw new IllegalArgumentException("File must not be empty.");
        // 结束当前代码块。
        }

        // 执行当前语句。
        String originalFilename = file.getOriginalFilename();
        // 执行当前流程控制分支。
        if (originalFilename == null) {
            // 抛出当前异常。
            throw new IllegalArgumentException("File name must not be empty.");
        // 结束当前代码块。
        }

        // 执行当前语句。
        String extension = getFileExtension(originalFilename).toLowerCase();
        // 执行当前流程控制分支。
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Unsupported file type: " + extension +
                    // 执行当前语句。
                    ". Supported types: " + String.join(", ", ALLOWED_EXTENSIONS));
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `getFileExtension` 操作。
     *
     * @param filename 输入参数 `filename`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String getFileExtension(String filename) {
        // 执行当前语句。
        int lastDotIndex = filename.lastIndexOf('.');
        // 执行当前流程控制分支。
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回当前结果。
            return "";
        // 结束当前代码块。
        }
        // 返回当前结果。
        return filename.substring(lastDotIndex + 1);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
