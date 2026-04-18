// 声明当前源文件所属包。
package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 让 Lombok 为当前类型生成常用访问方法。
@Data
// 让 Lombok 为当前类型生成建造者。
@Builder
// 让 Lombok 生成无参构造函数。
@NoArgsConstructor
// 让 Lombok 生成全参构造函数。
@AllArgsConstructor
// 定义问答响应对象，返回模型回答和引用来源。
public class InoteResponse {

    // 声明会话id变量，供后续流程使用。
    private String sessionId;
    // 声明回答变量，供后续流程使用。
    private String answer;
    // 声明来源变量，供后续流程使用。
    private List<SourceReference> sources;
}
