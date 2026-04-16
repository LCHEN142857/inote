// 声明当前源文件的包。
package com.inote.service;

import com.inote.model.entity.Document;
import com.inote.repository.DocumentRepository;
import com.inote.service.retrieval.BM25SearchService;
import com.inote.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class DocumentProcessingService {

    // 声明当前字段。
    private static final int CHUNK_SIZE = 1000;
    // 声明当前字段。
    private static final int CHUNK_OVERLAP = 200;

    // 声明当前字段。
    private final DocumentParser documentParser;
    // 声明当前字段。
    private final EmbeddingService embeddingService;
    // 声明当前字段。
    private final DocumentRepository documentRepository;
    // 声明当前字段。
    private final BM25SearchService bm25SearchService;

    /**
     * 描述 `processDocumentAsync` 操作。
     *
     * @param document 输入参数 `document`。
     * @param file 输入参数 `file`。
     * @param fileUrl 输入参数 `fileUrl`。
     * @return 无返回值。
     */
    // 应用当前注解。
    @Async
    // 处理当前代码结构。
    public void processDocumentAsync(Document document, MultipartFile file, String fileUrl) {
        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            log.info("Starting async processing for document: {}", document.getId());

            // 执行当前语句。
            document.setStatus("PARSING");
            // 执行当前语句。
            documentRepository.save(document);

            // 执行当前语句。
            String content = documentParser.parse(file);
            // 执行当前语句。
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            // 执行当前语句。
            log.info("Document {} split into {} chunks", document.getId(), chunks.size());

            // 执行当前语句。
            Map<String, Object> metadata = new HashMap<>();
            // 执行当前语句。
            metadata.put("document_id", document.getId());
            // 执行当前语句。
            metadata.put("file_name", document.getFileName());
            // 执行当前语句。
            metadata.put("file_url", fileUrl);
            // 执行当前语句。
            metadata.put("content_type", document.getContentType());
            // 执行当前语句。
            metadata.put("owner_id", document.getOwner().getId());

            // 执行当前语句。
            embeddingService.embedAndStore(chunks, metadata);

            // 执行当前流程控制分支。
            try {
                // 处理当前代码结构。
                List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                        // 处理当前代码结构。
                        .map(chunk -> {
                            // 执行当前语句。
                            Map<String, Object> docMeta = new HashMap<>(metadata);
                            // 执行当前语句。
                            docMeta.put("chunk_size", chunk.length());
                            // 返回当前结果。
                            return new org.springframework.ai.document.Document(chunk, docMeta);
                        // 处理当前代码结构。
                        })
                        // 执行当前语句。
                        .toList();
                // 执行当前语句。
                bm25SearchService.indexDocuments(aiDocuments);
            // 处理当前代码结构。
            } catch (Exception e) {
                // 执行当前语句。
                log.warn("BM25 indexing failed for document {}: {}", document.getId(), e.getMessage());
            // 结束当前代码块。
            }

            // 执行当前语句。
            document.setStatus("COMPLETED");
            // 执行当前语句。
            document.setErrorMessage(null);
            // 执行当前语句。
            documentRepository.save(document);
            // 执行当前语句。
            log.info("Document processing completed: {}", document.getId());
        // 处理当前代码结构。
        } catch (Exception e) {
            // 执行当前语句。
            log.error("Failed to process document: {}", document.getId(), e);
            // 执行当前语句。
            document.setStatus("FAILED");
            // 执行当前语句。
            document.setErrorMessage(e.getMessage());
            // 执行当前语句。
            documentRepository.save(document);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `saveFile` 操作。
     *
     * @param file 输入参数 `file`。
     * @param uploadPath 输入参数 `uploadPath`。
     * @return 类型为 `Path` 的返回值。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 处理当前代码结构。
    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        // 执行当前语句。
        String originalFilename = file.getOriginalFilename();
        // 执行当前语句。
        String fileExtension = getFileExtension(originalFilename);
        // 处理当前代码结构。
        String newFileName = fileExtension.isEmpty()
                // 处理当前代码结构。
                ? UUID.randomUUID().toString()
                // 执行当前语句。
                : UUID.randomUUID() + "." + fileExtension;

        // 执行当前语句。
        Path targetPath = Paths.get(uploadPath, newFileName);
        // 执行当前语句。
        Files.createDirectories(targetPath.getParent());
        // 执行当前流程控制分支。
        try (InputStream inputStream = file.getInputStream()) {
            // 执行当前语句。
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // 结束当前代码块。
        }
        // 返回当前结果。
        return targetPath;
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
        // 执行当前流程控制分支。
        if (filename == null) {
            // 返回当前结果。
            return "";
        // 结束当前代码块。
        }
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
