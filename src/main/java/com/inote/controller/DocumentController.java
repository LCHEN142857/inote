// 声明当前源文件的包。
package com.inote.controller;

import com.inote.model.dto.DocumentStatusResponse;
import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@RestController
// 应用当前注解。
@RequestMapping("/api/v1/documents")
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class DocumentController {

    // 声明当前字段。
    private final DocumentService documentService;

    // 应用当前注解。
    @Value("${file.upload.path:./uploads}")
    // 声明当前字段。
    private String uploadPath;

    /**
     * 描述 `uploadDocument` 操作。
     *
     * @param file 输入参数 `file`。
     * @return 类型为 `ResponseEntity<DocumentUploadResponse>` 的返回值。
     */
    // 应用当前注解。
    @PostMapping("/upload")
    // 处理当前代码结构。
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        // 执行当前语句。
        log.info("Received file upload request: {}", file.getOriginalFilename());
        // 执行当前语句。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 执行当前流程控制分支。
        if ("FAILED".equals(response.getStatus())) {
            // 返回当前结果。
            return ResponseEntity.badRequest().body(response);
        // 结束当前代码块。
        }
        // 返回当前结果。
        return ResponseEntity.ok(response);
    // 结束当前代码块。
    }

    /**
     * 描述 `listDocuments` 操作。
     *
     * @return 类型为 `ResponseEntity<List<DocumentStatusResponse>>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping
    // 处理当前代码结构。
    public ResponseEntity<List<DocumentStatusResponse>> listDocuments() {
        // 返回当前结果。
        return ResponseEntity.ok(documentService.listDocuments());
    // 结束当前代码块。
    }

    /**
     * 描述 `getDocumentStatus` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @return 类型为 `ResponseEntity<DocumentStatusResponse>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/{documentId}")
    // 处理当前代码结构。
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable String documentId) {
        // 返回当前结果。
        return ResponseEntity.ok(documentService.getDocumentStatus(documentId));
    // 结束当前代码块。
    }

    /**
     * 描述 `getFile` 操作。
     *
     * @param documentId 输入参数 `documentId`。
     * @return 类型为 `ResponseEntity<Resource>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/files/{documentId}")
    // 处理当前代码结构。
    public ResponseEntity<Resource> getFile(@PathVariable String documentId) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            Document document = documentService.getOwnedDocument(documentId);
            // 执行当前语句。
            Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
            // 执行当前语句。
            Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            // 执行当前流程控制分支。
            if (!filePath.startsWith(uploadRoot)) {
                // 执行当前语句。
                log.warn("Blocked file access outside upload directory: {}", documentId);
                // 返回当前结果。
                return ResponseEntity.badRequest().build();
            // 结束当前代码块。
            }

            // 执行当前语句。
            Resource resource = new UrlResource(filePath.toUri());
            // 执行当前流程控制分支。
            if (resource.exists() && resource.isReadable()) {
                // 返回当前结果。
                return ResponseEntity.ok()
                        // 处理当前代码结构。
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                        // 执行当前语句。
                        .body(resource);
            // 结束当前代码块。
            }
            // 返回当前结果。
            return ResponseEntity.notFound().build();
        // 处理当前代码结构。
        } catch (MalformedURLException e) {
            // 执行当前语句。
            log.error("Invalid file path for document: {}", documentId, e);
            // 返回当前结果。
            return ResponseEntity.badRequest().build();
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }
// 结束当前代码块。
}
