// 声明当前源文件的包。
package com.inote.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 应用当前注解。
@Getter
// 应用当前注解。
@Setter
// 应用当前注解。
@Component
// 应用当前注解。
@ConfigurationProperties(prefix = "app.auth")
// 声明当前类型。
public class AuthProperties {

    // 声明当前字段。
    private int captchaLength = 6;
// 结束当前代码块。
}
