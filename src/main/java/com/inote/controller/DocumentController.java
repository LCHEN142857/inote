// 声明包路径，控制器层
package com.inote.controller;

// 导入文档上传响应 DTO
import com.inote.model.dto.DocumentUploadResponse;
// 导入文档服务类
import com.inote.service.DocumentService;
// 导入 Lombok 构造函数注解
import lombok.RequiredArgsConstructor;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入配置值注入注解
import org.springframework.beans.factory.annotation.Value;
// 导入 Spring 资源抽象接口
import org.springframework.core.io.Resource;
// 导入基于 URL 的资源实现
import org.springframework.core.io.UrlResource;
// 导入 HTTP 头常量类
import org.springframework.http.HttpHeaders;
// 导入 MIME 类型常量类
import org.springframework.http.MediaType;
// 导入 Spring HTTP 响应实体
import org.springframework.http.ResponseEntity;
// 导入所有 Spring Web 注解
import org.springframework.web.bind.annotation.*;
// 导入文件上传接口
import org.springframework.web.multipart.MultipartFile;

// 导入 URL 格式异常类
import java.net.MalformedURLException;
// 导入文件路径类
import java.nio.file.Path;
// 导入路径工具类
import java.nio.file.Paths;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 REST 控制器
@RestController
// 设置基础路径为 /api/v1/documents
@RequestMapping("/api/v1/documents")
// 为 final 字段生成构造函数，实现依赖注入
@RequiredArgsConstructor
// 文档控制器，处理文档上传和下载的 HTTP 请求
public class DocumentController {

    // 注入文档服务，处理文档业务逻辑
    private final DocumentService documentService;

    // 从配置文件读取文件上传路径，默认值为 ./uploads
    @Value("${file.upload.path:./uploads}")
    // 文件上传存储目录路径
    private String uploadPath;

    /**
     * 上传文档接口
     * @param file 前端上传的文件（multipart/form-data 格式）
     * @return 上传响应，包含文档 ID 和处理状态
     */
    // 映射 POST /api/v1/documents/upload 请求
    @PostMapping("/upload")
    // 返回文档上传响应
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            // 从请求参数中获取名为 "file" 的上传文件
            @RequestParam("file") MultipartFile file) {
        // 记录上传的文件名
        log.info("Received file upload request: {}", file.getOriginalFilename());
        // 调用文档服务处理上传
        DocumentUploadResponse response = documentService.uploadDocument(file);

        // 如果文档处理状态为失败
        if ("FAILED".equals(response.getStatus())) {
            // 返回 400 Bad Request 响应
            return ResponseEntity.badRequest().body(response);
        }

        // 上传成功，返回 200 OK 响应
        return ResponseEntity.ok(response);
    }

    /**
     * 获取已上传文件接口
     * @param filename 文件名（路径变量）
     * @return 文件资源内容
     */
    // 映射 GET 请求，{filename:.+} 正则允许文件名包含点号（如 file.pdf）
    @GetMapping("/files/{filename:.+}")
    // 从 URL 路径中提取文件名参数
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            // 根据上传目录和文件名构建完整路径，normalize 消除路径中的 .. 防止路径遍历攻击
            Path filePath = Paths.get(uploadPath).resolve(filename).normalize();
            // 将文件路径转为 URL 资源对象
            Resource resource = new UrlResource(filePath.toUri());

            // 检查文件是否存在且可读
            if (resource.exists() && resource.isReadable()) {
                // 返回 200 OK 响应
                return ResponseEntity.ok()
                        // 设置 Content-Disposition 响应头，inline 表示在浏览器内直接展示而非下载
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + filename + "\"")
                        // 返回文件资源作为响应体
                        .body(resource);
            } else {
                // 文件不存在，返回 404 Not Found
                return ResponseEntity.notFound().build();
            }
        // 捕获 URL 格式错误异常
        } catch (MalformedURLException e) {
            // 记录错误日志
            log.error("Invalid file path: {}", filename, e);
            // 返回 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }
}
