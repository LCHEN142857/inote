// 声明当前源文件所属包。
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

// 启用当前类的日志记录能力。
@Slf4j
// 声明当前类提供 REST 风格接口。
@RestController
// 声明当前控制器的统一请求路径前缀。
@RequestMapping("/api/v1/documents")
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义文档接口控制器，负责上传、列表、状态和文件访问接口。
public class DocumentController {

    // 声明文档service变量，供后续流程使用。
    private final DocumentService documentService;

    // 从配置文件中注入当前字段的取值。
    @Value("${file.upload.path:./uploads}")
    // 声明上传path变量，供后续流程使用。
    private String uploadPath;

    /**
     * 接收用户上传的文件并启动文档处理流程。
     * @param file 文件参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 POST 请求。
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        // 记录当前流程的运行日志。
        log.info("Received file upload request: {}", file.getOriginalFilename());
        // 计算并保存响应结果。
        DocumentUploadResponse response = documentService.uploadDocument(file);
        // 根据条件判断当前分支是否执行。
        if ("FAILED".equals(response.getStatus())) {
            // 返回参数错误响应。
            return ResponseEntity.badRequest().body(response);
        }
        // 返回成功响应。
        return ResponseEntity.ok(response);
    }

    /**
     * 返回当前用户的文档列表。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping
    public ResponseEntity<List<DocumentStatusResponse>> listDocuments() {
        // 返回成功响应。
        return ResponseEntity.ok(documentService.listDocuments());
    }

    /**
     * 返回指定文档的处理状态。
     * @param documentId 文档id参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable String documentId) {
        // 返回成功响应。
        return ResponseEntity.ok(documentService.getDocumentStatus(documentId));
    }

    /**
     * 校验文件归属后输出原始文档内容。
     * @param documentId 文档id参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/files/{documentId}")
    public ResponseEntity<Resource> getFile(@PathVariable String documentId) {
        // 进入异常保护块执行关键逻辑。
        try {
            // 计算并保存文档结果。
            Document document = documentService.getOwnedDocument(documentId);
            // 计算并保存上传root结果。
            Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
            // 计算并保存文件path结果。
            Path filePath = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
            // 根据条件判断当前分支是否执行。
            if (!filePath.startsWith(uploadRoot)) {
                // 记录当前流程的运行日志。
                log.warn("Blocked file access outside upload directory: {}", documentId);
                // 返回参数错误响应。
                return ResponseEntity.badRequest().build();
            }

            // 创建resource对象。
            Resource resource = new UrlResource(filePath.toUri());
            // 根据条件判断当前分支是否执行。
            if (resource.exists() && resource.isReadable()) {
                // 返回成功响应。
                return ResponseEntity.ok()
                        // 设置header字段的取值。
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                        // 设置body字段的取值。
                        .body(resource);
            }
            // 返回未找到响应。
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            // 记录当前流程的运行日志。
            log.error("Invalid file path for document: {}", documentId, e);
            // 返回参数错误响应。
            return ResponseEntity.badRequest().build();
        }
    }
}
