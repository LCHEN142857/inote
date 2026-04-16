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
public class ResetPasswordRequest {

    // 应用当前注解。
    @NotBlank(message = "newPassword must not be blank")
    // 声明当前字段。
    private String newPassword;

    // 应用当前注解。
    @NotBlank(message = "confirmPassword must not be blank")
    // 声明当前字段。
    private String confirmPassword;
// 结束当前代码块。
}
