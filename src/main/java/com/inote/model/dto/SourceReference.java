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
// 定义引用来源对象，标识回答对应的文档来源。
public class SourceReference {

    // 声明文件name变量，供后续流程使用。
    private String fileName;
    // 声明url变量，供后续流程使用。
    private String url;
}
