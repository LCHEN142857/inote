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
// 定义重置密码请求对象，承载新旧密码确认信息。
public class ResetPasswordRequest {

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "newPassword must not be blank")
    // 声明new密码变量，供后续流程使用。
    private String newPassword;

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "confirmPassword must not be blank")
    // 声明confirm密码变量，供后续流程使用。
    private String confirmPassword;
}
