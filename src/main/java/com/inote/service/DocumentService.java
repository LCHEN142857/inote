package com.inote.service;

import com.inote.model.dto.DocumentUploadResponse;
import com.inote.model.entity.Document;
import com.inote.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService processingService;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "txt", "csv"
    );

    /**
     * 上传并处理文档
     * @param file 上传的文件
     * @return 上传响应
     */
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        try {
            // 1. 验证文件
            validateFile(file);

            // 2. 保存文件到本地
            Path savedPath = processingService.saveFile(file, uploadPath);
            String fileUrl = "/api/v1/documents/files/" + savedPath.getFileName().toString();

            // 3. 创建文档记录
            Document document = Document.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(savedPath.toString())
                    .fileUrl(fileUrl)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("PENDING")
                    .build();
            
            document = documentRepository.save(document);

            // 4. 异步处理文档
            processingService.processDocumentAsync(document, file, fileUrl);

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .status(document.getStatus())
                    .message("文档上传成功，正在处理中")
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("File validation failed: {}", e.getMessage());
            return DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        } catch (IOException e) {
            log.error("Failed to save file", e);
            return DocumentUploadResponse.builder()
                    .status("FAILED")
                    .message("文件保存失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件格式: " + extension + 
                    "。支持的格式: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
