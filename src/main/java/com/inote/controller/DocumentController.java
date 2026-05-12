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
import org.springframework.web.bind.annotation.DeleteMapping;
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

// 暴露文档上传、状态查询和文件预览接口。
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    // 委托业务层处理文档权限、持久化和解析调度。
    private final DocumentService documentService;

    // 文件上传根目录，用于校验文件预览路径边界。
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 接收用户上传的文档并触发异步解析。
     * @param file 前端提交的文档文件。
     * @return 上传受理结果。
     * @throws IllegalArgumentException 文件为空或类型不支持时抛出。
     * @throws DocumentStorageException 文件保存失败时抛出。
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        // 记录上传入口日志，便于排查用户上传文件名。
        log.info("Received file upload request: {}", file.getOriginalFilename());
        // 由业务层完成文件校验、保存和解析任务调度。
        return ResponseEntity.ok(documentService.uploadDocument(file));
    }

    /**
     * 查询当前用户的文档列表。
     * @return 当前用户文档状态列表。
     * @throws com.inote.security.UnauthorizedException 当前请求未认证时抛出。
     */
    @GetMapping
    public ResponseEntity<List<DocumentStatusResponse>> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    /**
     * 查询单个文档解析状态。
     * @param documentId 文档 ID。
     * @return 文档状态响应。
     * @throws jakarta.persistence.EntityNotFoundException 文档不存在或不属于当前用户时抛出。
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getDocumentStatus(documentId));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteFailedDocument(@PathVariable String documentId) {
        documentService.deleteFailedDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 返回当前用户拥有的文档文件资源。
     * @param documentId 文档 ID。
     * @return 可内联预览的文件资源响应。
     * @throws jakarta.persistence.EntityNotFoundException 文档不存在或不属于当前用户时抛出。
     */
    @GetMapping("/files/{documentId}")
    public ResponseEntity<Resource> getFile(@PathVariable String documentId) {
        try {
            // 先通过业务层校验文档归属，防止越权读取。
            Document document = documentService.getOwnedDocument(documentId);
            // 标准化上传根目录，用于后续路径穿越校验。
            Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
            // 标准化实际文件路径，避免相对路径绕过检查。
            Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            // 禁止返回上传目录之外的文件。
            if (!filePath.startsWith(uploadRoot)) {
                log.warn("Blocked file access outside upload directory: {}", documentId);
                return ResponseEntity.badRequest().build();
            }

            // 将磁盘文件包装成 Spring Resource。
            Resource resource = new UrlResource(filePath.toUri());
            // 文件存在且可读时返回内联预览响应。
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                        .body(resource);
            }
            // 数据库记录存在但文件缺失时返回 404。
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            // 非法文件路径无法转换成 URL 时返回错误请求。
            log.error("Invalid file path for document: {}", documentId, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
