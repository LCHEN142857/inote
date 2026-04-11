// 声明包路径，项目根包
package com.inote;

// 导入 Spring Boot 应用启动器
import org.springframework.boot.SpringApplication;
// 导入 Spring Boot 自动配置注解
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 导入启用异步方法执行的注解
import org.springframework.scheduling.annotation.EnableAsync;

// 标注为 Spring Boot 应用入口，启用自动配置、组件扫描和 Bean 定义
@SpringBootApplication
// 启用 Spring 异步方法执行能力，使 @Async 注解生效
@EnableAsync
// 应用主类
public class InoteApplication {

    // Java 程序入口方法
    public static void main(String[] args) {
        // 启动 Spring Boot 应用，初始化 Spring 容器
        SpringApplication.run(InoteApplication.class, args);
    }

}
