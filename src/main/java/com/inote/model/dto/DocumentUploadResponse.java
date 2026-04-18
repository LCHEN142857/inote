// 声明当前源文件所属包。
package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 让 Lombok 为当前类型生成常用访问方法。
@Data
// 让 Lombok 为当前类型生成建造者。
@Builder
// 让 Lombok 生成无参构造函数。
@NoArgsConstructor
// 让 Lombok 生成全参构造函数。
@AllArgsConstructor
// 定义文档上传响应对象，返回上传结果和处理状态。
public class DocumentUploadResponse {

    // 声明文档id变量，供后续流程使用。
    private String documentId;
    // 声明文件name变量，供后续流程使用。
    private String fileName;
    // 声明状态变量，供后续流程使用。
    private String status;
    // 声明消息变量，供后续流程使用。
    private String message;
}
