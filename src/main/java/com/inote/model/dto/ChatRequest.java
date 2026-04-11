// 声明包路径，DTO（数据传输对象）层
package com.inote.model.dto;

// 导入非空白校验注解，用于参数校验
import jakarta.validation.constraints.NotBlank;
// 导入 Lombok 注解，自动生成全参构造函数
import lombok.AllArgsConstructor;
// 导入 Lombok 注解，启用建造者模式
import lombok.Builder;
// 导入 Lombok 注解，自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;
// 导入 Lombok 注解，自动生成无参构造函数
import lombok.NoArgsConstructor;

// 自动生成 getter、setter、toString、equals、hashCode 方法
@Data
// 启用 Builder 建造者模式，支持链式构建对象
@Builder
// 生成无参构造函数，JSON 反序列化需要
@NoArgsConstructor
// 生成全参构造函数，Builder 模式需要
@AllArgsConstructor
// 聊天请求 DTO，接收前端发送的用户问题
public class ChatRequest {

    // 校验 question 字段不能为 null 且不能为空白字符串，校验失败时返回提示信息
    @NotBlank(message = "问题不能为空")
    // 用户输入的问题文本
    private String question;
}
