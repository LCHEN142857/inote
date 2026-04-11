// 声明包路径，配置层
package com.inote.config;

// 导入 Milvus 向量数据库客户端
import io.milvus.client.MilvusServiceClient;
// 导入 Milvus 连接参数构建器
import io.milvus.param.ConnectParam;
// 导入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 导入配置值注入注解
import org.springframework.beans.factory.annotation.Value;
// 导入 Bean 定义注解
import org.springframework.context.annotation.Bean;
// 导入配置类注解
import org.springframework.context.annotation.Configuration;

// 自动创建 Slf4j 日志对象 log
@Slf4j
// 标注为 Spring 配置类
@Configuration
// Milvus 向量数据库连接配置类
public class MilvusConfig {

    // 从配置文件读取 Milvus 服务器地址，默认 localhost
    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    // Milvus 服务器主机地址
    private String host;

    // 从配置文件读取 Milvus 端口号，默认 19530
    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    // Milvus 服务器端口号
    private int port;

    // 从配置文件读取数据库名称，默认 default
    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    // Milvus 数据库名称
    private String databaseName;

    // 将返回值注册为 Spring Bean，供 VectorStore 等组件使用
    @Bean
    // 创建 Milvus 客户端连接
    public MilvusServiceClient milvusClient() {
        // 记录 Milvus 初始化信息
        log.info("Initializing Milvus client: {}:{}", host, port);

        // 使用建造者模式构建连接参数
        ConnectParam connectParam = ConnectParam.newBuilder()
                // 设置 Milvus 服务器地址
                .withHost(host)
                // 设置 Milvus 服务器端口
                .withPort(port)
                // 设置要连接的数据库名称
                .withDatabaseName(databaseName)
                // 构建连接参数对象
                .build();

        // 创建并返回 Milvus 客户端实例
        return new MilvusServiceClient(connectParam);
    }
}
