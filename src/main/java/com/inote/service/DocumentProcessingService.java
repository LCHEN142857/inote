// 声明包路径，服务层
package com.inote.service;

// 导入文档实体类
import com.inote.model.entity.Document;
// 导入文档仓库
import com.inote.repository.DocumentRepository;
import com.inote.service.retrieval.BM25SearchService;
// 导入文档解析工具类
import com.inote.util.DocumentParser;
// 导入 Lombok 构造函数注解
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入 Spring 异步执行注解
import org.springframework.scheduling.annotation.Async;
// 导入 Spring 服务注解
import org.springframework.stereotype.Service;
// 导入文件上传接口
import org.springframework.web.multipart.MultipartFile;

// 导入 IO 异常类
import java.io.IOException;
// 导入 NIO 文件操作工具
import java.nio.file.Files;
// 导入文件路径类
import java.nio.file.Path;
// 导入路径工具类
import java.nio.file.Paths;
// 导入 HashMap 集合
import java.util.HashMap;
// 导入 List 集合接口
import java.util.List;
// 导入 Map 集合接口
import java.util.Map;
// 导入 UUID 工具类
import java.util.UUID;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 服务层组件
@Service
// 为所有 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
// 文档处理服务，负责文档解析、分块和向量化的异步处理
public class DocumentProcessingService {

    private final DocumentParser documentParser;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final BM25SearchService bm25SearchService;

    // 文本分块大小，每块最多 1000 个字符
    private static final int CHUNK_SIZE = 1000;
    // 相邻文本块之间的重叠字符数，保证上下文连续性
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 异步处理文档
     * 流程：解析文档内容 → 按字符数分块 → 向量化并存入 Milvus 向量数据库
     * @Async 标注使此方法在独立线程池中异步执行
     */
    // 标记为异步方法，由 Spring 在独立线程中执行，不阻塞调用方
    @Async
    public void processDocumentAsync(Document document, MultipartFile file, String fileUrl) {
        try {
            // 记录开始处理的日志
            log.info("Starting async processing for document: {}", document.getId());

            // 更新文档状态为"处理中"
            document.setStatus("PROCESSING");
            // 将状态更新保存到仓库
            documentRepository.save(document);

            // 1. 解析文档内容：根据文件类型（PDF/Word/Excel/TXT）提取纯文本
            String content = documentParser.parse(file);
            // 记录解析出的文本长度
            log.debug("Parsed content length: {} chars", content.length());

            // 2. 分块：将长文本按段落和字符数切分为多个小文本块，块大小 1000 字符，重叠 200 字符
            List<String> chunks = documentParser.chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
            // 记录分块数量
            log.info("Document split into {} chunks", chunks.size());

            // 3. 准备元数据：每个文本块都会携带这些元数据存入向量数据库
            Map<String, Object> metadata = new HashMap<>();
            // 文档 ID，用于关联原始文档
            metadata.put("document_id", document.getId());
            // 原始文件名，用于展示来源
            metadata.put("file_name", document.getFileName());
            // 文件访问 URL，用于来源引用
            metadata.put("file_url", fileUrl);
            // 文件 MIME 类型
            metadata.put("content_type", document.getContentType());

            // 4. 向量化并存入 Milvus：将文本块转为向量并存储
            embeddingService.embedAndStore(chunks, metadata);

            // 5. 同步 BM25 索引：为混合检索准备关键词索引
            // BM25 索引失败不应影响文档处理成功状态，仅记录警告日志
            try {
                List<org.springframework.ai.document.Document> aiDocuments = chunks.stream()
                        .map(chunk -> {
                            Map<String, Object> docMeta = new HashMap<>(metadata);
                            docMeta.put("chunk_size", chunk.length());
                            return new org.springframework.ai.document.Document(chunk, docMeta);
                        })
                        .toList();
                bm25SearchService.indexDocuments(aiDocuments);
            } catch (Exception e) {
                log.warn("BM25 indexing failed for document {}: {}", document.getId(), e.getMessage());
                // BM25 索引失败不影响整体文档处理流程
            }

            // 6. 更新文档状态为"完成"
            document.setStatus("COMPLETED");
            // 将状态更新保存到仓库
            documentRepository.save(document);

            // 记录处理完成的日志
            log.info("Document processing completed: {}", document.getId());

        // 捕获处理过程中的所有异常
        } catch (Exception e) {
            // 记录处理失败的错误日志
            log.error("Failed to process document: {}", document.getId(), e);
            // 设置状态为处理失败
            document.setStatus("FAILED");
            // 记录错误信息
            document.setErrorMessage(e.getMessage());
            // 将失败状态保存到仓库
            documentRepository.save(document);
        }
    }

    /**
     * 保存上传文件到本地磁盘
     * 文件名用 UUID 重命名，避免重名冲突
     */
    public Path saveFile(MultipartFile file, String uploadPath) throws IOException {
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 提取文件扩展名
        String fileExtension = getFileExtension(originalFilename);
        // 生成 UUID 文件名 + 原始扩展名，避免文件名冲突
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        // 拼接目标存储路径
        Path targetPath = Paths.get(uploadPath, newFileName);
        // 创建目标目录（如果不存在），包含所有父目录
        Files.createDirectories(targetPath.getParent());
        // 将上传文件的输入流复制到目标路径
        Files.copy(file.getInputStream(), targetPath);

        // 记录文件保存路径
        log.debug("File saved to: {}", targetPath);
        // 返回文件保存的完整路径
        return targetPath;
    }

    // 从文件名中提取扩展名
    private String getFileExtension(String filename) {
        // 如果文件名为 null
        if (filename == null) {
            // 返回空字符串
            return "";
        }
        // 查找最后一个点号的位置
        int lastDotIndex = filename.lastIndexOf('.');
        // 如果没有点号或点号在末尾
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回空字符串
            return "";
        }
        // 返回点号之后的部分作为扩展名
        return filename.substring(lastDotIndex + 1);
    }
}
