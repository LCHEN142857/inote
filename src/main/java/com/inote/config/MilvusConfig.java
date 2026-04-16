// 声明当前源文件的包。
package com.inote.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 应用当前注解。
@Slf4j
// 应用当前注解。
@Configuration
// 声明当前类型。
public class MilvusConfig {

    // 应用当前注解。
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    // 声明当前字段。
    private String host;

    // 应用当前注解。
    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    // 声明当前字段。
    private int port;

    // 应用当前注解。
    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    // 声明当前字段。
    private String databaseName;

    /**
     * 描述 `milvusClient` 操作。
     *
     * @return 类型为 `MilvusServiceClient` 的返回值。
     */
    // 应用当前注解。
    @Bean
    // 处理当前代码结构。
    public MilvusServiceClient milvusClient() {
        // 执行当前语句。
        log.info("Initializing Milvus client: {}:{}", host, port);

        // 处理当前代码结构。
        ConnectParam connectParam = ConnectParam.newBuilder()
                // 处理当前代码结构。
                .withHost(host)
                // 处理当前代码结构。
                .withPort(port)
                // 处理当前代码结构。
                .withDatabaseName(databaseName)
                // 执行当前语句。
                .build();

        // 返回当前结果。
        return new MilvusServiceClient(connectParam);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
