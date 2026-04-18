// 声明当前源文件所属包。
package com.inote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// 应用 `SpringBootApplication` 注解声明当前行为。
@SpringBootApplication
// 应用 `EnableAsync` 注解声明当前行为。
@EnableAsync
// 定义 Spring Boot 应用启动入口。
public class InoteApplication {

    /**
     * 处理main相关逻辑。
     * @param args args参数。
     */
    public static void main(String[] args) {
        // 定义当前类型。
        SpringApplication.run(InoteApplication.class, args);
    }

}
