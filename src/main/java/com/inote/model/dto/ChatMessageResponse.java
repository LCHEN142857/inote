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
// 定义聊天消息响应对象，返回消息明细。
public class ChatMessageResponse {

    // 声明id变量，供后续流程使用。
    private String id;
    // 声明role变量，供后续流程使用。
    private String role;
    // 声明内容变量，供后续流程使用。
    private String content;
    // 声明createdat变量，供后续流程使用。
    private LocalDateTime createdAt;
}
