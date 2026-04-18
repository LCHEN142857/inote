// 声明当前源文件所属包。
package com.inote.model.dto;

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
// 定义验证码响应对象，返回验证码标识和验证码内容。
public class AuthCaptchaResponse {

    // 声明验证码id变量，供后续流程使用。
    private String captchaId;
    // 声明验证码code变量，供后续流程使用。
    private String captchaCode;
}
