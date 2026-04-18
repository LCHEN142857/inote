// 声明当前源文件所属包。
package com.inote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 声明当前类提供 Spring 配置。
@Configuration
// 定义密码编码配置，负责提供密码加密器。
public class PasswordEncoderConfig {

    /**
     * 处理密码encoder相关逻辑。
     * @return 密码encoder结果。
     */
    // 声明当前方法向容器提供 Bean。
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 返回 `BCryptPasswordEncoder` 的处理结果。
        return new BCryptPasswordEncoder();
    }
}
