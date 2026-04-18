// 声明当前源文件所属包。
package com.inote.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 启用当前类的日志记录能力。
@Slf4j
// 声明当前类提供 Spring 配置。
@Configuration
// 定义向量库配置，负责初始化 Milvus 相关 Bean。
public class MilvusConfig {

    // 从配置文件中注入当前字段的取值。
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    // 声明host变量，供后续流程使用。
    private String host;

    // 从配置文件中注入当前字段的取值。
    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    // 声明port变量，供后续流程使用。
    private int port;

    // 从配置文件中注入当前字段的取值。
    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    // 声明databasename变量，供后续流程使用。
    private String databaseName;

    /**
     * 处理milvusclient相关逻辑。
     * @return milvusserviceclient结果。
     */
    // 声明当前方法向容器提供 Bean。
    @Bean
    public MilvusServiceClient milvusClient() {
        // 记录当前流程的运行日志。
        log.info("Initializing Milvus client: {}:{}", host, port);

        // 围绕connectparamconnect补充当前业务语句。
        ConnectParam connectParam = ConnectParam.newBuilder()
                // 设置withhost字段的取值。
                .withHost(host)
                // 设置withport字段的取值。
                .withPort(port)
                // 设置withdatabasename字段的取值。
                .withDatabaseName(databaseName)
                // 完成当前建造者对象的组装。
                .build();

        // 返回 `MilvusServiceClient` 的处理结果。
        return new MilvusServiceClient(connectParam);
    }
}
