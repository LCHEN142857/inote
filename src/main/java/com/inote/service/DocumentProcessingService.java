// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义文档处理服务，负责异步解析文档并建立检索索引。
public class DocumentProcessingService {

    // 计算并保存分块size结果。
    private static final int CHUNK_SIZE = 1000;
    // 计算并保存分块overlap结果。
    private static final int CHUNK_OVERLAP = 200;

    // 声明文档parser变量，供后续流程使用。
    private final DocumentParser documentParser;
    // 声明向量service变量，供后续流程使用。
    private final EmbeddingService embeddingService;
    // 声明文档repository变量，供后续流程使用。
    private final DocumentRepository documentRepository;
    // 声明BM25searchservice变量，供后续流程使用。
    private final BM25SearchService bm25SearchService;

    /**
     * 异步解析文档内容，并写入向量库和 BM25 索引。
     * @param document 文档参数。
     * @param file 文件参数。
     * @param fileUrl 文件url参数。
     */
    // 声明当前方法以异步方式执行。
    @Async
    public void processDocumentAsync(Document document, MultipartFile file, String fileUrl) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 记录当前流程的运行日志。
            log.info("Starting async processing for document: {}", document.getId());

            // 更新状态字段。
            document.setStatus("PARSING");
            // 保存当前对象到持久化层。
            documentRepository.save(document);

            // 计算并保存内容结果。
            String content = documentParser.parse(file);
            // 计算并保存分块结果。
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            // 记录当前流程的运行日志。
            log.info("Document {} split into {} chunks", document.getId(), chunks.size());

            // 创建元数据对象。
            Map<String, Object> metadata = new HashMap<>();
            // 写入当前映射中的键值对。
            metadata.put("document_id", document.getId());
            // 写入当前映射中的键值对。
            metadata.put("file_name", document.getFileName());
            // 写入当前映射中的键值对。
            metadata.put("file_url", fileUrl);
            // 写入当前映射中的键值对。
            metadata.put("content_type", document.getContentType());
            // 写入当前映射中的键值对。
            metadata.put("owner_id", document.getOwner().getId());

            // 调用 `embedAndStore` 完成当前步骤。
            embeddingService.embedAndStore(chunks, metadata);

            // 进入异常保护块执行关键逻辑。
            try {
                // 围绕orgspringframeworkai补充当前业务语句。
                List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                        // 设置map字段的取值。
                        .map(chunk -> {
                            // 创建docmeta对象。
                            Map<String, Object> docMeta = new HashMap<>(metadata);
                            // 写入当前映射中的键值对。
                            docMeta.put("chunk_size", chunk.length());
                            // 返回 `Document` 的处理结果。
                            return new org.springframework.ai.document.Document(chunk, docMeta);
                        })
                        // 设置tolist字段的取值。
                        .toList();
                // 调用 `indexDocuments` 完成当前步骤。
                bm25SearchService.indexDocuments(aiDocuments);
            } catch (Exception e) {
                // 记录当前流程的运行日志。
                log.warn("BM25 indexing failed for document {}: {}", document.getId(), e.getMessage());
            }

            // 更新状态字段。
            document.setStatus("COMPLETED");
            // 更新错误信息消息字段。
            document.setErrorMessage(null);
            // 保存当前对象到持久化层。
            documentRepository.save(document);
            // 记录当前流程的运行日志。
            log.info("Document processing completed: {}", document.getId());
        } catch (Exception e) {
            // 记录当前流程的运行日志。
            log.error("Failed to process document: {}", document.getId(), e);
            // 更新状态字段。
            document.setStatus("FAILED");
            // 更新错误信息消息字段。
            document.setErrorMessage(e.getMessage());
            // 保存当前对象到持久化层。
            documentRepository.save(document);
        }
    }

    /**
     * 将上传文件保存到本地存储目录。
     * @param file 文件参数。
     * @param uploadPath 上传path参数。
     * @return 保存后的文件路径。
     * @throws IOException 文件读写失败时抛出。
     */
    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        // 计算并保存originalfilename结果。
        String originalFilename = file.getOriginalFilename();
        // 计算并保存文件extension结果。
        String fileExtension = getFileExtension(originalFilename);
        // 围绕文件name文件补充当前业务语句。
        String newFileName = fileExtension.isEmpty()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + fileExtension;

        // 计算并保存targetpath结果。
        Path targetPath = Paths.get(uploadPath, newFileName);
        // 调用 `createDirectories` 完成当前步骤。
        Files.createDirectories(targetPath.getParent());
        // 进入异常保护块执行关键逻辑。
        try (InputStream inputStream = file.getInputStream()) {
            // 调用 `copy` 完成当前步骤。
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        // 返回targetpath。
        return targetPath;
    }

    /**
     * 处理get文件extension相关逻辑。
     * @param filename filename参数。
     * @return 处理后的字符串结果。
     */
    private String getFileExtension(String filename) {
        // 根据条件判断当前分支是否执行。
        if (filename == null) {
            // 返回""。
            return "";
        }
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
