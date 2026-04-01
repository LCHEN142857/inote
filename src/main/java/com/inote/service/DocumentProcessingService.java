package com.inote.service;

import com.inote.model.entity.Document;
import com.inote.repository.DocumentRepository;
import com.inote.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentParser documentParser;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 异步处理文档
     * 1. 解析文档内容
     * 2. 分块
     * 3. 向量化并存入 Milvus
     */
    @Async
    public void processDocumentAsync(Document document, MultipartFile file, String fileUrl) {
        try {
            log.info("Starting async processing for document: {}", document.getId());
            
            // 更新状态为处理中
            document.setStatus("PROCESSING");
            documentRepository.save(document);

            // 1. 解析文档内容
            String content = documentParser.parse(file);
            log.debug("Parsed content length: {} chars", content.length());

            // 2. 分块
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            log.info("Document split into {} chunks", chunks.size());

            // 3. 准备元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", document.getId());
            metadata.put("file_name", document.getFileName());
            metadata.put("file_url", fileUrl);
            metadata.put("content_type", document.getContentType());

            // 4. 向量化并存入 Milvus
            embeddingService.embedAndStore(chunks, metadata);

            // 5. 更新状态为完成
            document.setStatus("COMPLETED");
            documentRepository.save(document);
            
            log.info("Document processing completed: {}", document.getId());
            
        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getId(), e);
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }

    /**
     * 保存文件到本地
     */
    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        Path targetPath = Paths.get(uploadPath, newFileName);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath);
        
        log.debug("File saved to: {}", targetPath);
        return targetPath;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
