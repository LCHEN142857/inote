// 声明包路径，DTO 层
package com.inote.model.dto;

// 导入 Lombok 全参构造函数注解
import lombok.AllArgsConstructor;
// 导入 Lombok 建造者模式注解
import lombok.Builder;
// 导入 Lombok 数据类注解
import lombok.Data;
// 导入 Lombok 无参构造函数注解
import lombok.NoArgsConstructor;

// 自动生成 getter、setter、toString、equals、hashCode 方法
@Data
// 启用 Builder 建造者模式
@Builder
// 生成无参构造函数
@NoArgsConstructor
// 生成全参构造函数
@AllArgsConstructor
// 文档上传响应 DTO，返回给前端上传结果
public class DocumentUploadResponse {

    // 文档唯一标识 ID
    private String documentId;
    // 上传的原始文件名
    private String fileName;
    // 文档处理状态：PENDING（待处理）/ PROCESSING（处理中）/ COMPLETED（完成）/ FAILED（失败）
    private String status;
    // 附加提示信息，如成功或失败原因
    private String message;
}
