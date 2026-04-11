// 声明包路径，实体层
package com.inote.model.entity;

// 导入 Lombok 全参构造函数注解
import lombok.AllArgsConstructor;
// 导入 Lombok 建造者模式注解
import lombok.Builder;
// 导入 Lombok 数据类注解
import lombok.Data;
// 导入 Lombok 无参构造函数注解
import lombok.NoArgsConstructor;

// 导入日期时间类型
import java.time.LocalDateTime;

// 自动生成 getter、setter、toString、equals、hashCode 方法
@Data
// 启用 Builder 建造者模式
@Builder
// 生成无参构造函数
@NoArgsConstructor
// 生成全参构造函数
@AllArgsConstructor
// 文档实体类，表示上传到系统中的一个文档记录
public class Document {

    // 文档唯一标识，由 UUID 生成
    private String id;
    // 原始文件名
    private String fileName;
    // 文件在服务器上的存储路径
    private String filePath;
    // 文件的 HTTP 访问地址
    private String fileUrl;
    // 文件 MIME 类型，如 application/pdf
    private String contentType;
    // 文件大小，单位字节
    private long fileSize;
    // 文档处理状态：PENDING（待处理）/ PROCESSING（处理中）/ COMPLETED（完成）/ FAILED（失败）
    private String status;
    // 处理失败时的错误信息
    private String errorMessage;
    // 文档创建时间
    private LocalDateTime createdAt;
    // 文档最后更新时间
    private LocalDateTime updatedAt;
}
