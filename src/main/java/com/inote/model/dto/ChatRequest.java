// 声明当前源文件所属包。
package com.inote.model.dto;

import jakarta.validation.constraints.NotBlank;
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
// 定义问答请求对象，承载会话标识和问题内容。
public class ChatRequest {

    // 声明会话id变量，供后续流程使用。
    private String sessionId;

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "question must not be blank")
    // 声明问题变量，供后续流程使用。
    private String question;
}
