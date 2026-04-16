// 声明当前源文件的包。
package com.inote.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 应用当前注解。
@Data
// 应用当前注解。
@Builder
// 应用当前注解。
@NoArgsConstructor
// 应用当前注解。
@AllArgsConstructor
// 声明当前类型。
public class AuthLoginRequest {

    // 应用当前注解。
    @NotBlank(message = "username must not be blank")
    // 声明当前字段。
    private String username;

    // 应用当前注解。
    @NotBlank(message = "password must not be blank")
    // 声明当前字段。
    private String password;

    // 应用当前注解。
    @NotBlank(message = "captchaId must not be blank")
    // 声明当前字段。
    private String captchaId;

    // 应用当前注解。
    @NotBlank(message = "captchaCode must not be blank")
    // 声明当前字段。
    private String captchaCode;
// 结束当前代码块。
}
