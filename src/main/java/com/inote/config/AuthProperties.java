// 声明当前源文件所属包。
package com.inote.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 应用 `Getter` 注解声明当前行为。
@Getter
// 应用 `Setter` 注解声明当前行为。
@Setter
// 将当前类注册为通用组件。
@Component
// 应用 `ConfigurationProperties` 注解声明当前行为。
@ConfigurationProperties(prefix = "app.auth")
// 定义认证配置属性，负责承载验证码相关参数。
public class AuthProperties {

    // 计算并保存验证码length结果。
    private int captchaLength = 6;
}
