// 声明包路径，服务层
package com.inote.service;

// 导入文档上传响应 DTO
import com.inote.model.dto.DocumentUploadResponse;
// 导入文档实体类
import com.inote.model.entity.Document;
// 导入文档仓库
import com.inote.repository.DocumentRepository;
// 导入 Lombok 构造函数注解
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入配置值注入注解
import org.springframework.beans.factory.annotation.Value;
// 导入 Spring 服务注解
import org.springframework.stereotype.Service;
// 导入文件上传接口
import org.springframework.web.multipart.MultipartFile;

// 导入 IO 异常类
import java.io.IOException;
// 导入文件路径类
import java.nio.file.Path;
// 导入 Set 集合接口
import java.util.Set;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 服务层组件
@Service
// 为所有 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
// 文档服务类，处理文档上传和校验的业务逻辑
public class DocumentService {

    // 注入文档仓库，用于持久化文档记录
    private final DocumentRepository documentRepository;
    // 注入文档处理服务，用于异步解析和向量化文档
    private final DocumentProcessingService processingService;

    // 从配置文件读取文件上传路径，默认 ./uploads
    @Value("${file.upload.path:./uploads}")
    // 文件上传存储目录
    private String uploadPath;

    // 允许上传的文件扩展名白名单（不可变集合），支持 PDF、Word、Excel、纯文本、CSV 格式
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "txt", "csv"
    );

    /**
     * 上传并处理文档
     * 流程：校验文件 → 保存到本地 → 创建文档记录 → 异步解析和向量化
     * @param file 前端上传的文件
     * @return 上传响应
     */
    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        try {
            // 1. 验证文件格式和内容
            validateFile(file);

            // 2. 保存文件到本地磁盘
            Path savedPath = processingService.saveFile(file, uploadPath);
            // 拼接文件的 HTTP 访问 URL
            String fileUrl = "/api/v1/documents/files/" + savedPath.getFileName().toString();

            // 3. 创建文档元数据记录
            Document document = Document.builder()
                    // 设置原始文件名
                    .fileName(file.getOriginalFilename())
                    // 设置文件存储路径
                    .filePath(savedPath.toString())
                    // 设置文件访问 URL
                    .fileUrl(fileUrl)
                    // 设置文件 MIME 类型
                    .contentType(file.getContentType())
                    // 设置文件大小（字节）
                    .fileSize(file.getSize())
                    // 设置初始状态为待处理
                    .status("PENDING")
                    // 构建文档实体对象
                    .build();

            // 保存文档记录到仓库（生成 ID 和时间戳）
            document = documentRepository.save(document);

            // 4. 异步处理文档（解析内容 → 分块 → 向量化 → 存入 Milvus），不阻塞当前请求
            processingService.processDocumentAsync(document, file, fileUrl);

            // 构建成功响应
            return DocumentUploadResponse.builder()
                    // 返回生成的文档 ID
                    .documentId(document.getId())
                    // 返回文件名
                    .fileName(document.getFileName())
                    // 返回当前状态（PENDING）
                    .status(document.getStatus())
                    // 返回提示信息
                    .message("文档上传成功，正在处理中")
                    // 构建响应对象
                    .build();

        // 捕获文件校验失败异常
        } catch (IllegalArgumentException e) {
            // 记录校验失败的警告日志
            log.warn("File validation failed: {}", e.getMessage());
            // 构建失败响应
            return DocumentUploadResponse.builder()
                    // 设置状态为失败
                    .status("FAILED")
                    // 返回校验失败的具体原因
                    .message(e.getMessage())
                    // 构建响应对象
                    .build();
        // 捕获文件保存失败的 IO 异常
        } catch (IOException e) {
            // 记录文件保存失败的错误日志
            log.error("Failed to save file", e);
            // 构建失败响应
            return DocumentUploadResponse.builder()
                    // 设置状态为失败
                    .status("FAILED")
                    // 返回 IO 错误信息
                    .message("文件保存失败: " + e.getMessage())
                    // 构建响应对象
                    .build();
        }
    }

    /**
     * 验证上传文件的合法性
     * 检查项：文件非空、文件名非空、文件格式在白名单中
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为 null 或内容为空
        if (file == null || file.isEmpty()) {
            // 抛出非法参数异常
            throw new IllegalArgumentException("文件不能为空");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 检查文件名是否为 null
        if (originalFilename == null) {
            // 抛出非法参数异常
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 提取文件扩展名并转为小写
        String extension = getFileExtension(originalFilename).toLowerCase();
        // 检查扩展名是否在允许的白名单中
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            // 抛出异常，提示不支持的格式并列出所有支持的格式
            throw new IllegalArgumentException("不支持的文件格式: " + extension +
                    "。支持的格式: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    // 从文件名中提取扩展名
    private String getFileExtension(String filename) {
        // 查找最后一个点号的位置
        int lastDotIndex = filename.lastIndexOf('.');
        // 如果没有点号或点号在末尾
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            // 返回空字符串，表示没有扩展名
            return "";
        }
        // 返回点号之后的部分作为扩展名
        return filename.substring(lastDotIndex + 1);
    }
}
