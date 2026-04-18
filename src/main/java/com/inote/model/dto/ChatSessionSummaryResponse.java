// 声明当前源文件所属包。
package com.inote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 让 Lombok 为当前类型生成常用访问方法。
@Data
// 让 Lombok 为当前类型生成建造者。
@Builder
// 让 Lombok 生成无参构造函数。
@NoArgsConstructor
// 让 Lombok 生成全参构造函数。
@AllArgsConstructor
// 定义会话摘要响应对象，返回列表页展示信息。
public class ChatSessionSummaryResponse {

    // 声明id变量，供后续流程使用。
    private String id;
    // 声明标题变量，供后续流程使用。
    private String title;
    // 声明消息count变量，供后续流程使用。
    private long messageCount;
    // 声明createdat变量，供后续流程使用。
    private LocalDateTime createdAt;
    // 声明updatedat变量，供后续流程使用。
    private LocalDateTime updatedAt;
}
