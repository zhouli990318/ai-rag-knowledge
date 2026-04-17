# AI RAG Platform

基于 DDD + 六边形架构的 AI RAG 平台，集成多供应商大模型、PgVector 知识库检索、MCP 网关与 React 管理界面。

## Features

- 多供应商模型切换：`OPENAI`、`OLLAMA`、`ANTHROPIC`、`ZHIPUAI`、`DEEPSEEK`、`DASHSCOPE`、`QIANFAN`、`MOONSHOT`
- 流式聊天：`/api/v1/chat/stream`（SSE）与 `/api/v1/chat/streamable`（NDJSON）
- RAG 知识库：文件上传、Git 导入、文本切分、PgVector 相似检索
- MCP 网关：导入 OpenAPI 后自动生成工具映射，并通过 REST / MCP 暴露
- Web 控制台：聊天、知识库、MCP 配置、供应商管理

## Modules

```text
backend/
├── shared-kernel/   # 通用响应、异常、错误码、加密
├── server/          # chat / provider / knowledge / vector search
└── mcp-gateway/     # api source / tool mapping / MCP server

frontend/            # React + Vite 控制台
uploads/             # 本地上传文件目录
```

- `backend/server`：主业务服务，端口 `8091`
- `backend/mcp-gateway`：MCP / REST 网关，端口 `8092`
- `frontend`：开发端口 `5173`

## Architecture

后端采用 DDD / Hexagonal 分层：

- `domain/**`：领域模型与端口
- `application/service/**`：应用编排
- `infrastructure/**`：JPA、AI 模型、向量库、MCP 适配器
- `interfaces/**`：REST Controller / DTO

典型链路：

- Chat：`ChatController` -> `ChatAppService` -> `ChatModelPort`
- RAG：`ChatAppService` -> `RetrievalDomainService` -> `VectorStorePort`
- MCP：`McpGatewayController` -> `McpGatewayAppService` -> `ToolInvocationDomainService`

## Tech Stack

- Backend：Java 25, Spring Boot 3.5.13, Spring AI 1.1.4, JPA, Flyway
- Frontend：React 19, TypeScript, Vite 6, MUI 6, TanStack Query, Zustand
- Infra：PostgreSQL 16 + PgVector, Redis 7

## Quick Start

### 1) Start infrastructure

```powershell
cd D:\progrem\code\public\SpringAiRAG
docker-compose up -d
```

### 2) Override local environment

```powershell
$env:APP_DATASOURCE_URL="jdbc:postgresql://localhost:5442/ai_rag_platform"
$env:APP_DATASOURCE_USERNAME="postgres"
$env:APP_DATASOURCE_PASSWORD="123456"
$env:APP_REDIS_HOST="localhost"
$env:APP_REDIS_PORT="6379"
$env:APP_MCP_GATEWAY_BASE_URL="http://localhost:8092/api/v1/mcp"
```

### 3) Start backend

```powershell
cd D:\progrem\code\public\SpringAiRAG\backend
mvn clean install -DskipTests
```

```powershell
cd D:\progrem\code\public\SpringAiRAG\backend\server
mvn spring-boot:run
```

```powershell
cd D:\progrem\code\public\SpringAiRAG\backend\mcp-gateway
mvn spring-boot:run
```

### 4) Start frontend

```powershell
cd D:\progrem\code\public\SpringAiRAG\frontend
npm install
npm run dev
```

Open `http://localhost:5173`

## Development

```powershell
cd D:\progrem\code\public\SpringAiRAG\backend
mvn test
```

```powershell
cd D:\progrem\code\public\SpringAiRAG\frontend
npm run build
```

## API Notes

- 前端通过 Vite 代理联调：`/api -> 8091`，`/mcp-api -> 8092/api`
- 大多数 REST 接口返回统一结构：`code / message / data / timestamp`
- 流式聊天接口不走统一包裹，前端直接消费 SSE 文本块与最终 `[DONE]`
- MCP Gateway 使用独立数据库 schema：`mcp_gateway`
- 上传文件默认保存在 `./uploads/<knowledgeBaseId>/...`
