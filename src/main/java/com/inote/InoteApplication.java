// 声明当前源文件的包。
package com.inote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// 应用当前注解。
@SpringBootApplication
// 应用当前注解。
@EnableAsync
// 声明当前类型。
public class InoteApplication {

    /**
     * 描述 `main` 操作。
     *
     * @param args 输入参数 `args`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public static void main(String[] args) {
        // 执行当前语句。
        SpringApplication.run(InoteApplication.class, args);
    // 结束当前代码块。
    }

// 结束当前代码块。
}
