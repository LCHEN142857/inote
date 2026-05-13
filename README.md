# inote 开发文档

inote 是一个面向个人/团队文档知识库的 RAG 问答系统。用户登录后上传 PDF、Word、Excel、TXT、CSV 文件，后端异步解析并写入向量库和 BM25 索引，聊天时按当前用户检索相关文档，结合会话历史调用大模型生成答案，并返回引用来源。

本文档基于当前仓库代码整理，重点帮助新人完成本地启动、前后端联调和代码定位。未在仓库中体现的能力会明确标注。

## 技术栈

| 层 | 技术 | 位置 |
| --- | --- | --- |
| 前端 | React 18、TypeScript、Vite 5、原生 CSS | `web/` |
| 后端 | Java 21、Spring Boot 3.4.1、Spring Web、Validation、JPA | `src/main/java/com/inote/` |
| AI/RAG | Spring AI 1.0.0-M5、OpenAI 兼容 Chat/Embedding、Milvus Vector Store、Lucene BM25 | `config/`、`service/retrieval/` |
| 数据库 | PostgreSQL 运行时、H2 测试 | `application.yml`、`application-test.yml` |
| 文件解析 | Apache PDFBox、Apache POI | `DocumentParser.java` |
| 构建测试 | Maven、npm、TypeScript | `pom.xml`、`web/package.json` |

当前仓库未体现 Redis、外部消息队列、Dockerfile、docker-compose 或部署脚本。`app/` 目录存在但当前为空。

## 仓库结构 / 模块划分

```text
inote/
├── pom.xml                         # 后端 Maven 工程
├── README.md                       # 本文档
├── project_spec.md                 # 项目规格说明
├── src/
│   ├── main/
│   │   ├── java/com/inote/
│   │   │   ├── InoteApplication.java
│   │   │   ├── controller/          # REST 接口入口
│   │   │   ├── service/             # 认证、文档、聊天、RAG 业务
│   │   │   ├── service/retrieval/   # 查询改写、拆分、混合检索、重排
│   │   │   ├── repository/          # Spring Data JPA 仓储
│   │   │   ├── model/entity/        # JPA 实体
│   │   │   ├── model/dto/           # 请求/响应 DTO
│   │   │   ├── security/            # Token 鉴权、限流、防重放
│   │   │   ├── config/              # AI、Milvus、异常、配置属性
│   │   │   ├── util/                # 文档解析工具
│   │   │   └── client/              # 备用模型客户端
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/com/inote/          # 控制器、服务、集成、持久层测试
│       └── resources/
│           ├── application-test.yml
│           └── test-files/
├── web/
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api.ts
│       ├── types.ts
│       ├── styles.css
│       └── components/
└── uploads/                         # 本地上传目录，运行时生成/使用
```

## 前端项目

### 前端结构

| 路径 | 作用 |
| --- | --- |
| `web/src/main.tsx` | React 挂载入口，渲染 `App` |
| `web/src/App.tsx` | 顶层状态管理：登录态、验证码、会话、文档、模型选择、上传、发送问题 |
| `web/src/api.ts` | 所有后端 API 调用封装；统一加 `X-Auth-Token`；处理 401 登出 |
| `web/src/types.ts` | 前后端 DTO 类型声明 |
| `web/src/components/AuthView.tsx` | 登录/注册页，验证码输入 |
| `web/src/components/SessionSidebar.tsx` | 会话列表、新建、重命名、删除、置顶、本地搜索、改密码、退出 |
| `web/src/components/ChatWorkspace.tsx` | 聊天主界面、SSE 流式回答、模型选择、引用模式开关 |
| `web/src/components/KnowledgePanel.tsx` | 文档上传、文档状态、失败文档删除、最近引用 |
| `web/src/utils.ts` | 消息构造、格式化、置顶会话本地存储辅助 |
| `web/src/styles.css` | 全局样式 |

前端没有使用路由库。`App.tsx` 通过 `window.history` 在 `/login` 和 `/index` 之间切换。

### 前端启动方式

```powershell
cd web
npm install
npm run dev
```

默认访问地址是 `http://localhost:5173`。`web/vite.config.ts` 将 `/api` 代理到 `http://localhost:8080`，所以本地联调时前端代码直接请求 `/api/v1/...`。

### 前端环境变量

当前前端代码未读取 `import.meta.env`，仓库也未体现 `.env` 文件。后端地址通过 Vite dev server proxy 配置在 `web/vite.config.ts` 中：

```ts
server: {
  port: 5173,
  proxy: {
    "/api": {
      target: "http://localhost:8080",
      changeOrigin: true
    }
  }
}
```

如果后端端口变化，修改这里的 `target`。

### 前端接口调用方式

`web/src/api.ts` 中的 `request<T>()` 统一处理 JSON 请求。登录成功后 `api.setToken()` 会把 token 写入 `localStorage` 的 `inote-auth-token`，后续请求自动带上：

```http
X-Auth-Token: <token>
```

上传文档使用 `FormData`，不会设置 JSON `Content-Type`。流式问答使用 `fetch("/api/v1/chat/query/stream")` 读取 SSE 格式事件，支持 `metadata`、`delta`、`done`。

## 后端项目

### 后端结构

| 包 | 作用 |
| --- | --- |
| `controller` | REST API：认证、聊天、文档 |
| `service` | 业务层：登录注册、会话、问答、文档上传/处理、Embedding、用户设置 |
| `service/retrieval` | RAG 检索流水线：查询改写、多查询拆分、向量检索、BM25、RRF 融合、重排 |
| `repository` | `User`、`Document`、`ChatSession`、`ChatMessage` 的 JPA 仓储 |
| `model/entity` | 数据表实体，主键为 UUID 字符串 |
| `model/dto` | 接口请求和响应结构 |
| `security` | `X-Auth-Token` 鉴权、当前用户上下文、限流、防重放、请求体缓存 |
| `config` | AI/Milvus Bean、配置属性、异常处理、密码编码器 |
| `util` | 文档解析与切块 |
| `client` | 主模型失败时调用备用模型 |

### 后端服务入口

启动类是 `src/main/java/com/inote/InoteApplication.java`，包含 `@SpringBootApplication` 和 `@EnableAsync`。`@EnableAsync` 用于文档上传后的异步解析入库。

本地启动命令：

```powershell
mvn.cmd spring-boot:run
```

默认端口是 `8080`，配置见 `src/main/resources/application.yml`。

### 配置依赖

后端运行依赖：

| 依赖 | 用途 | 配置 |
| --- | --- | --- |
| PostgreSQL | 存用户、文档元数据、会话、消息 | `spring.datasource.*` |
| Milvus | 存文档切块向量 | `spring.ai.vectorstore.milvus.*` |
| OpenAI 兼容 Chat/Embedding API | 聊天和向量化 | `spring.ai.openai.*` |
| Moonshot/Kimi 兼容接口 | 主模型失败后的备用聊天模型 | `ai.fallback.*` |
| 本地磁盘 | 保存上传原文件 | `file.upload.path`，默认 `./uploads` |

当前仓库未体现缓存中间件和消息队列。登录验证码、登录锁、限流、防重放缓存均在内存中实现，服务重启后会丢失。

### 数据模型

| 实体 | 表 | 说明 |
| --- | --- | --- |
| `User` | `users` | 用户名、密码 hash、认证 token、回答模式设置 |
| `Document` | `documents` | 上传文件元数据、磁盘路径、文件 URL、解析状态、所属用户、是否当前有效版本 |
| `ChatSession` | `chat_sessions` | 用户会话，删除会话级联删除消息 |
| `ChatMessage` | `chat_messages` | 用户/助手消息，助手消息可保存引用来源 JSON |

JPA 配置为 `hibernate.ddl-auto: update`，本地启动会按实体自动调整表结构；生产迁移工具当前仓库未体现。

## 接口清单

除 `GET /api/v1/auth/captcha` 和 `POST /api/v1/auth/login` 外，`/api/v1/**` 都需要请求头 `X-Auth-Token`。

| Method | Path | 作用 | 前端调用位置 | 后端实现位置 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/auth/captcha` | 获取登录验证码图片和验证码 ID | `web/src/api.ts` 的 `getCaptcha`，`App.tsx`/`AuthView.tsx` | `AuthController.captcha` |
| POST | `/api/v1/auth/login` | 登录；用户不存在时自动注册；返回 token | `api.login`，`App.tsx` 的 `handleLogin` | `AuthController.login`、`AuthService.loginOrRegister` |
| GET | `/api/v1/auth/me` | 根据 token 获取当前用户 | `api.me`，`App.tsx` 的 `bootstrapAuth` | `AuthController.me` |
| POST | `/api/v1/auth/password/reset` | 当前用户重置密码并刷新 token | `api.resetPassword`，`SessionSidebar` 经 `App.tsx` 调用 | `AuthController.resetPassword`、`AuthService.resetPassword` |
| GET | `/api/v1/auth/settings` | 获取当前用户回答模式设置 | `api.getSettings`，`App.tsx` 的 `bootstrapWorkspace` | `AuthController.settings`、`UserSettingsService` |
| POST | `/api/v1/auth/settings` | 更新是否仅根据参考文档回答 | `api.updateSettings`，`ChatWorkspace` 经 `App.tsx` 调用 | `AuthController.updateSettings`、`UserSettingsService` |
| GET | `/api/v1/chat/models` | 获取默认模型和可选模型列表 | `api.getChatModels`，`App.tsx` 的 `bootstrapWorkspace` | `ChatController.models`、`ChatModelSelectionService.catalog` |
| POST | `/api/v1/chat/sessions` | 新建会话，可传 `title` | `api.createSession`，`App.tsx` 的 `handleNewSession`/发送前补会话 | `ChatController.createSession`、`ChatSessionService.createSession` |
| GET | `/api/v1/chat/sessions` | 获取当前用户会话摘要列表 | `api.listSessions`，`App.tsx` 的 `bootstrapWorkspace`/`refreshSessions` | `ChatController.listSessions`、`ChatSessionService.listSessions` |
| GET | `/api/v1/chat/sessions/{sessionId}` | 获取会话详情和消息列表 | `api.getSession`，`App.tsx` 的 `handleSelectSession` | `ChatController.getSession`、`ChatSessionService.getSession` |
| PUT | `/api/v1/chat/sessions/{sessionId}` | 重命名会话 | `api.updateSession`，`App.tsx` 的 `handleRenameSubmit` | `ChatController.updateSession`、`ChatSessionService.updateSession` |
| DELETE | `/api/v1/chat/sessions/{sessionId}` | 删除会话 | `api.deleteSession`，`App.tsx` 的 `handleDeleteSession` | `ChatController.deleteSession`、`ChatSessionService.deleteSession` |
| POST | `/api/v1/chat/query` | 非流式问答，返回完整答案 | `api.query` 当前封装存在，当前 UI 主要使用流式接口 | `ChatController.query`、`ChatService.query` |
| POST | `/api/v1/chat/query/stream` | 流式问答，SSE 事件为 `metadata`、`delta`、`done` | `api.queryStream`，`App.tsx` 的 `handleSend` | `ChatController.streamQuery`、`ChatService.streamQuery` |
| POST | `/api/v1/documents/upload` | 上传文档并触发异步解析 | `api.uploadDocument`，`KnowledgePanel` 经 `App.tsx` 调用 | `DocumentController.uploadDocument`、`DocumentService.uploadDocument` |
| GET | `/api/v1/documents` | 获取当前用户文档列表 | `api.listDocuments`，`App.tsx` 的 `bootstrapWorkspace`/轮询刷新 | `DocumentController.listDocuments`、`DocumentService.listDocuments` |
| GET | `/api/v1/documents/{documentId}` | 获取单个文档解析状态 | 当前前端未直接调用 | `DocumentController.getDocumentStatus` |
| DELETE | `/api/v1/documents/delete/{documentId}` | 删除解析失败的文档 | `api.deleteDocument`，`App.tsx` 的 `handleDeleteFailedDocument` | `DocumentController.deleteFailedDocument`、`DocumentService.deleteFailedDocument` |
| GET | `/api/v1/documents/files/{documentId}` | 预览/读取当前用户拥有的原文件 | 引用 `SourceReference.url` 返回该 URL；当前 UI 展示来源预览，未直接打开文件 | `DocumentController.getFile` |

常见错误响应由 `GlobalExceptionHandler` 统一处理：400 参数错误、401 未认证、404 资源不存在、413 文件过大、423 登录锁定、500 未预期异常。限流会返回 429，防重放会返回 409。

## 核心业务流程

### 登录 / 自动注册

1. 前端访问时先调用 `GET /api/v1/auth/captcha`。
2. 用户提交用户名、密码、验证码，调用 `POST /api/v1/auth/login`。
3. `AuthService` 校验验证码和登录锁；用户名不存在则创建用户，存在则校验密码。
4. 登录成功生成新的 `authToken`，前端保存到 `localStorage`。
5. 后续请求由 `AuthContextFilter` 读取 `X-Auth-Token`，把当前用户放入 `CurrentUserHolder`。

用户名规则为 3-32 位字母、数字、下划线或短横线；密码至少 6 位。验证码有效期 5 分钟。

### 文档上传和入库

1. 前端 `KnowledgePanel` 选择文件，`api.uploadDocument` 以 `multipart/form-data` 调用 `/api/v1/documents/upload`。
2. `DocumentService` 校验文件非空和扩展名，仅支持 `pdf`、`docx`、`xlsx`、`xls`、`txt`、`csv`。
3. `DocumentProcessingService.saveFile` 将文件保存到 `file.upload.path`，使用 UUID 文件名避免覆盖。
4. 数据库写入 `Document`，状态为 `PARSING`；同一用户同名旧文档会被标记为 `active=false`。
5. `processDocumentAsync` 异步解析文本、按 1000 字符切块并保留 200 字符重叠。
6. `EmbeddingService` 写入 Milvus；`BM25SearchService` 写入进程内 Lucene 索引。
7. 成功后文档状态改为 `COMPLETED`，失败则改为 `FAILED`。
8. 前端每 3 秒轮询 `/api/v1/documents` 刷新仍在处理的文档状态。

注意：BM25 使用内存索引 `ByteBuffersDirectory`，服务重启后内存索引不会从数据库自动重建；向量数据保存在 Milvus。

### 聊天问答

1. 前端发送问题时优先调用流式接口 `/api/v1/chat/query/stream`。
2. `ChatService` 根据请求模型名通过 `ChatModelSelectionService` 解析为允许的模型，不合法则回退默认模型。
3. `RetrievalPipelineService` 可执行查询改写、多查询拆分，再调用 `HybridRetrievalService`。
4. 混合检索先查 Milvus 向量，再按配置查 BM25，并用 RRF 融合；只保留当前用户且 `active=true` 的文档。
5. 如启用 rerank，则由 `RerankService` 重排；否则取 `finalTopK`。
6. 有参考文档时构造 RAG prompt；无文档且用户设置“仅根据参考文档回答”时直接返回无法回答；否则走通用知识回答。
7. 主模型失败时 `FallbackChatModel` 尝试备用模型。
8. 流式接口先发 `metadata`，再发多次 `delta`，最后发 `done`，并持久化用户消息、助手消息和引用来源。

## 配置文件说明

| 文件 | 说明 |
| --- | --- |
| `src/main/resources/application.yml` | 主配置，定义端口、数据库、AI、Milvus、RAG、模型列表、上传路径、日志级别 |
| `src/main/resources/application-local.yml` | 本地 profile 覆盖配置；当前仓库文件包含外部服务地址和明文密钥，使用时建议改为环境变量或本机私有配置，不要继续扩散 |
| `src/main/resources/application-prod.yml` | prod profile 覆盖配置；当前仓库文件同样包含外部服务地址和明文密钥，发布前应改造为安全注入 |
| `src/test/resources/application-test.yml` | 测试配置，使用 H2 内存库，排除 OpenAI/Milvus 自动配置，上传目录为 `./target/test-uploads` |
| `src/main/resources/logback-spring.xml` | 控制台和文件日志。文件日志路径固定为 `/app/inote/logs/server/inote-server.log` |
| `web/vite.config.ts` | 前端 dev server 端口和 `/api` 代理 |
| `web/tsconfig*.json` | TypeScript 编译配置 |
| `web/package.json` | 前端脚本和依赖 |
| `pom.xml` | 后端依赖、Java 版本、Spring Boot 插件 |

主要环境变量：

| 变量 | 默认值 | 作用 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/inote` | PostgreSQL JDBC 地址 |
| `DB_USERNAME` | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `ALI_API_KEY` | `your-api-key-here` | DashScope/OpenAI 兼容 API Key |
| `KIMI_API_KEY` | `your-kimi-api-key-here` | 备用模型 API Key |
| `MILVUS_HOST` | `localhost` | Milvus 主机 |
| `MILVUS_PORT` | `19530` | Milvus 端口 |
| `app.chat.default-model` | `kimi-k2.6` | 默认聊天模型，可通过 JVM 参数或配置覆盖 |

## 环境准备

1. 安装 JDK 21。
2. 安装 Maven 3.8+，或使用本机可用的 `mvn.cmd`。
3. 安装 Node.js 18+ 和 npm。
4. 准备 PostgreSQL，并创建数据库 `inote`。
5. 准备 Milvus 2.x，默认端口 `19530`。
6. 准备 OpenAI 兼容的大模型和 embedding 服务密钥。当前配置默认使用 DashScope 兼容地址。

本仓库没有提供 Milvus/PostgreSQL 的 docker-compose，如何启动这些外部服务当前仓库未体现。

## 本地启动

### 1. 后端

PowerShell 示例：

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/inote"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:ALI_API_KEY="<your-dashscope-key>"
$env:KIMI_API_KEY="<your-kimi-key>"
$env:MILVUS_HOST="localhost"
$env:MILVUS_PORT="19530"

mvn.cmd spring-boot:run
```

后端启动成功后监听 `http://localhost:8080`。

### 2. 前端

另开一个终端：

```powershell
cd web
npm install
npm run dev
```

访问 `http://localhost:5173/login`。第一次登录时，如果用户名不存在会自动注册。

### 3. 联调检查

1. 打开前端登录页，确认验证码图片能显示。
2. 登录后创建会话。
3. 上传一个 `txt` 或 `pdf`，观察右侧文档状态从 `PARSING` 变为 `COMPLETED`。
4. 在聊天框提问，确认请求走 `/api/v1/chat/query/stream`，回答带引用来源。

## 前后端如何联调

本地推荐使用 Vite 代理：

```text
Browser -> http://localhost:5173 -> /api/* -> http://localhost:8080
```

联调时重点看三处：

| 问题 | 定位位置 |
| --- | --- |
| 前端请求路径、请求体、token | `web/src/api.ts` |
| 页面状态、何时调用接口 | `web/src/App.tsx` |
| 后端接口入口和业务分发 | `controller/*Controller.java` |

前端没有配置 CORS，因为开发态由 Vite proxy 转发，浏览器看到的是同源请求。如果绕过 Vite 直接从其他域访问 `localhost:8080`，当前仓库未体现专门的 CORS 配置。

## 打包发布

### 后端打包

```powershell
mvn.cmd clean package
```

产物通常在 `target/` 下生成 Spring Boot 可执行 jar。运行示例：

```powershell
java -jar target/inote-1.0.0.jar
```

发布环境需要自行提供 PostgreSQL、Milvus、模型 API 配置和上传目录。当前仓库未体现 Dockerfile、systemd、Kubernetes、Nginx 或 CI/CD 发布脚本。

### 前端打包

```powershell
cd web
npm install
npm run build
```

产物输出到 `web/dist/`。当前后端代码未体现自动托管 `web/dist` 静态资源的配置；生产部署时需要单独用 Nginx/静态服务器托管，或新增 Spring Boot 静态资源集成。

## 测试方式

后端测试：

```powershell
mvn.cmd test
```

测试配置使用 `src/test/resources/application-test.yml`：

| 测试范围 | 文件 |
| --- | --- |
| 控制器 | `AuthControllerTest`、`ChatControllerTest`、`DocumentControllerTest` |
| 服务层 | `AuthServiceTest`、`ChatServiceTest`、`DocumentServiceTest` |
| 检索 | `HybridRetrievalServiceTest`、`RetrievalPipelineServiceTest` |
| 安全过滤器 | `AuthContextFilterTest` |
| 持久层 | `PersistenceLayerTest` |
| 集成流程 | `BackendFlowIntegrationTest` |

前端可执行：

```powershell
cd web
npm run build
```

当前仓库未体现前端单元测试或 E2E 测试脚本。

## 新人上手建议

1. 先从 `web/src/api.ts` 和三个 Controller 对照接口，理解页面动作如何进入后端。
2. 看 `AuthContextFilter`，明确除验证码和登录外所有 API 都依赖 `X-Auth-Token`。
3. 看 `DocumentService` 和 `DocumentProcessingService`，理解上传、异步解析、状态变化和入库。
4. 看 `ChatService` 和 `RetrievalPipelineService`，理解提问时如何检索、构造 prompt、流式返回。
5. 看 `application.yml` 中的 `rag` 和 `app.chat`，这些配置直接影响召回数量、阈值、模型列表和默认模型。
6. 修改接口时同步更新 `web/src/types.ts`、`web/src/api.ts`、对应 Controller/DTO，并补控制器或服务测试。

## 常见问题与注意事项

| 问题 | 说明 |
| --- | --- |
| 登录后接口 401 | 检查前端 `localStorage` 是否有 `inote-auth-token`，请求头是否带 `X-Auth-Token`，后端 `users.auth_token` 是否匹配 |
| 文档一直 `PARSING` | 看后端日志，常见原因是文件解析失败、Embedding API 失败、Milvus 不可用 |
| 文档 `FAILED` | 前端只允许删除失败文档；后端限制见 `DocumentService.deleteFailedDocument` |
| 上传同名文件后旧文件不再参与检索 | 代码会把同一用户同名旧版本设为 `active=false` |
| 重启后 BM25 结果变少 | BM25 索引是内存索引，重启后不会自动重建；向量检索仍依赖 Milvus |
| 模型列表没有前端选项 | 前端从 `/api/v1/chat/models` 读取，后端配置在 `app.chat.available-models` |
| 非流式接口是否还在用 | `POST /api/v1/chat/query` 后端和前端封装存在，但当前 UI 主要调用流式接口 |
| 日志文件写入失败 | `logback-spring.xml` 默认写 `/app/inote/logs/server/inote-server.log`，本地没有权限或目录不存在时需要调整日志配置 |
| 配置文件有敏感信息 | 当前 `application-local.yml`、`application-prod.yml` 包含明文密钥/外部地址；新人使用时应改为环境变量或本机私有配置 |

## License

MIT License，见 `LICENSE`。
