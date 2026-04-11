# inote 智能知识库问答系统

基于 Spring Boot 3 + Spring AI Alibaba 构建的 RAG 架构知识问答系统。

## 功能特性

- 📄 **文档上传与解析**：支持 PDF、Word、Excel、TXT、CSV 格式
- 🔍 **智能问答**：基于向量检索的 RAG 问答，返回精准答案
- 🔗 **溯源功能**：回答附带引用文档来源链接
- 🔄 **容错机制**：主模型失败自动切换到备选模型
- 📊 **向量存储**：基于 Milvus 向量数据库

## 技术栈

- Java 21
- Spring Boot 3.4.x
- Spring AI Alibaba 1.0.0-M6
- Milvus 向量数据库
- 阿里云大模型 (qwen3-max / kimi-2.5)

## 快速开始

### 1. 环境准备

- JDK 21+
- Maven 3.8+
- Milvus 2.x 向量数据库

### 2. 配置环境变量

```bash
export ALI_API_KEY=your-dashscope-api-key
export MILVUS_HOST=localhost
export MILVUS_PORT=19530
export KIMI_API_KEY=your-moonshot-api-key  # 可选，用于备选模型
```

### 3. 编译运行

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

### 4. 使用 Docker 运行 Milvus

```bash
# 下载 Milvus 启动脚本
curl -sfL https://raw.githubusercontent.com/milvus-io/milvus/master/scripts/standalone_embed.sh -o standalone_embed.sh

# 启动 Milvus
bash standalone_embed.sh start
```

## API 接口

### 1. 上传文档

```bash
POST /api/v1/documents/upload
Content-Type: multipart/form-data

file: <文件>
```

**响应示例：**
```json
{
  "documentId": "uuid",
  "fileName": "文档.pdf",
  "status": "PENDING",
  "message": "文档上传成功，正在处理中"
}
```

### 2. 知识库问答

```bash
POST /api/v1/chat/query
Content-Type: application/json

{
  "question": "你的问题"
}
```

**响应示例：**
```json
{
  "answer": "根据文档内容生成的回答...",
  "sources": [
    {"fileName": "用户手册.pdf", "url": "/api/v1/documents/files/xxx.pdf"},
    {"fileName": "技术文档.docx", "url": "/api/v1/documents/files/yyy.docx"}
  ]
}
```

## 项目结构

```
inote/
├── src/main/java/com/inote/
│   ├── InoteApplication.java      # 启动类
│   ├── config/                    # 配置类
│   │   ├── AiConfig.java          # AI 配置
│   │   ├── MilvusConfig.java      # Milvus 配置
│   │   └── GlobalExceptionHandler.java
│   ├── controller/                # 控制器
│   │   ├── DocumentController.java
│   │   └── ChatController.java
│   ├── service/                   # 服务层
│   │   ├── DocumentService.java
│   │   ├── DocumentProcessingService.java
│   │   ├── EmbeddingService.java
│   │   └── ChatService.java
│   ├── client/                    # 客户端
│   │   └── FallbackChatModel.java # 容错模型
│   ├── model/                     # 模型
│   │   ├── dto/                   # 数据传输对象
│   │   └── entity/                # 实体
│   ├── repository/                # 仓库
│   └── util/                      # 工具类
│       └── DocumentParser.java    # 文档解析
└── src/main/resources/
    └── application.yml            # 配置文件
```

## 配置说明

修改 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${ALI_API_KEY}  # 阿里云 DashScope API Key（OpenAI 兼容模式）
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    vectorstore:
      milvus:
        host: ${MILVUS_HOST:localhost}
        port: ${MILVUS_PORT:19530}

ai:
  fallback:
    model: kimi-2.5
    api-key: ${KIMI_API_KEY}  # Moonshot API Key
```

## 模型容错机制

系统默认使用 `qwen3-max` 作为主模型，当调用失败（超时、5xx错误等）时，会自动切换到 `kimi-2.5` 备选模型，整个过程对上层业务透明。

## 许可证

MIT License
