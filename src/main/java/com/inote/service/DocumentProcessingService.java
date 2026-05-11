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

// 负责文档落盘、解析、切块和向量化索引调度。
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    // 控制语义切块的最大长度。
    private static final int CHUNK_SIZE = 1000;
    // 相邻切块之间保留的重叠字符数。
    private static final int CHUNK_OVERLAP = 200;

    // 提供多格式文档解析能力。
    private final DocumentParser documentParser;
    // 提供向量化存储能力。
    private final EmbeddingService embeddingService;
    // 持久化文档状态变更。
    private final DocumentRepository documentRepository;
    // 维护 BM25 检索索引。
    private final BM25SearchService bm25SearchService;

    /**
     * 异步解析并索引文档内容。
     * @param documentId 文档 ID。
     * @param fileUrl 文档对外访问地址。
     */
    @Async
    public void processDocumentAsync(String documentId, String fileUrl) {
        // 先根据 ID 取出文档记录，缺失则直接跳过。
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("Skipping async processing for missing document {}", documentId);
            return;
        }

        try {
            // 记录解析开始状态，便于前端轮询查看。
            log.info("Starting async processing for document: {}", document.getId());
            document.setStatus("PARSING");
            documentRepository.save(document);

            // 从磁盘读取实际文件内容并解析为纯文本。
            Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            String content = documentParser.parse(filePath, document.getFileName());
            // 将长文本拆成适合检索和向量化的块。
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            log.info("Document {} split into {} chunks", document.getId(), chunks.size());

            // 准备通用元数据，供向量库和检索索引使用。
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", document.getId());
            metadata.put("file_name", document.getFileName());
            metadata.put("file_url", fileUrl);
            metadata.put("content_type", document.getContentType());
            metadata.put("owner_id", document.getOwner().getId());

            // 将切块内容写入 embedding 存储。
            embeddingService.embedAndStore(chunks, metadata);

            try {
                // 转成 Spring AI 文档对象后写入 BM25 索引。
                List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                        .map(chunk -> {
                            Map<String, Object> docMeta = new HashMap<>(metadata);
                            docMeta.put("chunk_size", chunk.length());
                            return new org.springframework.ai.document.Document(chunk, docMeta);
                        })
                        .toList();
                bm25SearchService.indexDocuments(aiDocuments);
            } catch (Exception e) {
                // BM25 失败不影响主流程，继续保留向量存储结果。
                log.warn("BM25 indexing failed for document {}: {}", document.getId(), e.getMessage());
            }

            // 所有处理成功后标记完成并清空错误信息。
            document.setStatus("COMPLETED");
            document.setErrorMessage(null);
            documentRepository.save(document);
            log.info("Document processing completed: {}", document.getId());
        } catch (Exception e) {
            // 任意解析或索引失败都要回写失败状态，供前端展示。
            log.error("Failed to process document: {}", document.getId(), e);
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }

    /**
     * 将上传文件保存到磁盘。
     * @param file 上传文件。
     * @param uploadPath 上传目录。
     * @return 保存后的文件路径。
     * @throws IOException 文件读写失败时抛出。
     */
    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        // 原始文件名只用于提取扩展名，不作为保存文件名。
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        // 使用随机文件名避免重名覆盖。
        String newFileName = fileExtension.isEmpty()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + fileExtension;

        // 统一归一化上传根目录和目标路径，防止路径穿越。
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetPath = uploadRoot.resolve(newFileName).normalize();
        // 确保上传目录存在。
        Files.createDirectories(uploadRoot);
        // 使用流式复制写入文件。
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    /**
     * 提取文件扩展名。
     * @param filename 原始文件名。
     * @return 扩展名，缺失时返回空串。
     */
    private String getFileExtension(String filename) {
        // 原始文件名为空时返回空扩展名。
        if (filename == null) {
            return "";
        }
        // 没有扩展名或以点结尾时返回空串。
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
