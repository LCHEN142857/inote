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
// 定义登录请求对象，承载用户名、密码和验证码信息。
public class AuthLoginRequest {

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "username must not be blank")
    // 声明username变量，供后续流程使用。
    private String username;

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "password must not be blank")
    // 声明密码变量，供后续流程使用。
    private String password;

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "captchaId must not be blank")
    // 声明验证码id变量，供后续流程使用。
    private String captchaId;

    // 应用 `NotBlank` 注解声明当前行为。
    @NotBlank(message = "captchaCode must not be blank")
    // 声明验证码code变量，供后续流程使用。
    private String captchaCode;
}
