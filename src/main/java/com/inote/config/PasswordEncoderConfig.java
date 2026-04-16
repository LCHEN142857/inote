// 声明当前源文件的包。
package com.inote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 应用当前注解。
@Configuration
// 声明当前类型。
public class PasswordEncoderConfig {

    /**
     * 描述 `passwordEncoder` 操作。
     *
     * @return 类型为 `PasswordEncoder` 的返回值。
     */
    // 应用当前注解。
    @Bean
    // 处理当前代码结构。
    public PasswordEncoder passwordEncoder() {
        // 返回当前结果。
        return new BCryptPasswordEncoder();
    // 结束当前代码块。
    }
// 结束当前代码块。
}
