// 声明当前源文件的包。
package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 应用当前注解。
@Data
// 应用当前注解。
@Builder
// 应用当前注解。
@NoArgsConstructor
// 应用当前注解。
@AllArgsConstructor
// 声明当前类型。
public class InoteResponse {

    // 声明当前字段。
    private String sessionId;
    // 声明当前字段。
    private String answer;
    // 声明当前字段。
    private List<SourceReference> sources;
// 结束当前代码块。
}
