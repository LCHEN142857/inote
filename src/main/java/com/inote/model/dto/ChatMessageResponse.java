// 声明当前源文件的包。
package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 应用当前注解。
@Data
// 应用当前注解。
@Builder
// 应用当前注解。
@NoArgsConstructor
// 应用当前注解。
@AllArgsConstructor
// 声明当前类型。
public class ChatMessageResponse {

    // 声明当前字段。
    private String id;
    // 声明当前字段。
    private String role;
    // 声明当前字段。
    private String content;
    // 声明当前字段。
    private LocalDateTime createdAt;
// 结束当前代码块。
}
