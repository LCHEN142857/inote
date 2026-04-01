# Role
你是一位精通 Java 生态和 AI 应用架构的高级后端开发工程师。请基于 Spring Boot 3 和 Spring AI Alibaba 框架，为我构建一个名为 `inote` 的智能知识库问答系统。

# Project Overview
- **项目名称**: inote
- **核心功能**: 这是一个基于 RAG (检索增强生成) 架构的知识问答系统。用户上传文档后，系统需解析并存入向量数据库。用户提问时，系统检索相关知识，利用大模型生成精准回答，并必须标注引用的来源文档链接。

# Tech Stack
- **语言**: Java 21
- **构建工具**: Maven
- **框架**: Spring Boot 3.x + Spring AI Alibaba
- **向量数据库**: Milvus
- **LLM 提供商**: Alibaba Cloud (兼容 OpenAI 协议)

# Key Requirements & Architecture

## 1. 文档处理与 RAG 管道
- **接口功能**: 提供文件上传接口。
- **支持格式**: Word (.docx), PDF (.pdf), Excel (.xlsx), TXT (.txt), CSV (.csv)。
- **处理流程**: 上传 -> 文本提取/解析 -> 分块 (Chunking) -> 向量化 (Embedding) -> 存入 Milvus。
- **元数据要求**: 存入向量库时，必须保留文件的元数据（如文件名、文件ID、URL路径），以便后续溯源。

## 2. 智能问答与溯源
- **接口功能**: 接收用户问题，返回答案及来源。
- **检索逻辑**: 基于用户问题在 Milvus 中进行向量检索 (Top-K)。
- **生成逻辑**: 将检索到的上下文与问题组装成 Prompt，发送给大模型。
- **溯源要求**: 响应结果中必须包含 `answer` (回答内容) 和 `sources` (引用文档列表，包含文档名和链接)。

## 3. 高可用大模型路由策略
- **模型配置**:
    - 主模型: `qwen3-max`
    - 备选模型: `kimi-2.5`
- **容错机制**: 实现一个自定义的 `ChatClient` 或拦截器。当主模型调用失败（如超时、5xx错误）时，系统应自动立即重试调用备选模型，对上层业务透明。

# API Design Specification

请实现以下 RESTful 接口：

1.  **POST /api/v1/documents/upload**
    - 描述: 上传并解析文档。
    - 输入: MultipartFile
    - 输出: DocumentID, Status

2.  **POST /api/v1/chat/query**
    - 描述: 知识库问答。
    - 输入: { "question": "用户的问题" }
    - 输出:
        ```json
        {
          "answer": "根据文档内容生成的回答...",
          "sources": [
            {"fileName": "用户手册.pdf", "url": "/files/123"},
            {"fileName": "技术文档.docx", "url": "/files/456"}
          ]
        }
        ```

# Implementation Guidelines
1.  请使用 Spring AI Alibaba 的 `ChatClient` 进行大模型交互。
2.  向量数据库操作请使用 `MilvusVectorStore`。
3.  请编写一个 `FallbackChatModel` 类来处理模型切换逻辑。
4.  代码结构需清晰，分层为 Controller, Service, Repository/Client。
5.  请提供 `application.yml` 配置示例，包含 Milvus 连接信息和 LLM API Key 配置。
