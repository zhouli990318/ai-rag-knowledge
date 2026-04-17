# AGENTS.md

## Repo snapshot
- Monorepo with three Maven modules in `backend/` and one Vite app in `frontend/`.
- `backend/server` (port `8091`) owns providers, chat, conversations, knowledge bases, and vector search.
- `backend/mcp-gateway` (port `8092`) owns API sources/tool mappings and exposes MCP + REST.
- `backend/shared-kernel` contains shared response/error/crypto primitives such as `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler`, and `CryptoUtil`.
- Both Java services share PostgreSQL; the gateway uses the separate `mcp_gateway` schema (`init-db.sql`, `backend/mcp-gateway/.../V1__init_mcp_gateway.sql`).

## Architecture and data flow
- The backend uses DDD + hexagonal structure: `domain/**` for models/ports, `application/service/**` for orchestration, `infrastructure/**` for adapters/config, `interfaces/**` for HTTP DTOs/controllers.
- Keep domain ports pure and add adapters rather than wiring JPA/HTTP directly into services; example: `KnowledgeBaseRepository` -> `KnowledgeBaseRepositoryAdapter` -> `JpaKnowledgeBaseRepository`.
- Chat flow is `ChatController` -> `ChatAppService` -> `ChatModelPort`; RAG context is appended via `RetrievalDomainService`, and MCP tools are injected by `McpToolCallbackService`.
- Document ingest flow is upload/Git import in `KnowledgeBaseAppService`, then parse -> split -> vectorize in `DocumentProcessingDomainService`; vector metadata keys include `knowledge_base_id`, `document_id`, `file_name`, `file_type`.
- MCP flow is frontend `/mcp-api` -> `McpGatewayController` -> `McpGatewayAppService`; parsed OpenAPI operations become `ToolMapping` rows, and actual HTTP execution happens in `ToolInvocationDomainService`.
- The main server consumes gateway-defined tools through `McpToolGatewayClient`, using `app.mcp-gateway.base-url` (default `http://localhost:8092/api/v1/mcp`).

## API and frontend conventions
- Most REST endpoints return `ApiResponse<T>` with `code/message/data/timestamp` (`backend/shared-kernel/src/main/java/com/silver/ai/shared/result/ApiResponse.java`).
- Streaming chat is the exception: `POST /api/v1/chat/stream` emits SSE text chunks plus a final `[DONE]`; `POST /api/v1/chat/streamable` emits NDJSON.
- The frontend relies on Vite proxying, not hardcoded base URLs: `/api` -> `http://localhost:8091`, `/mcp-api` -> `http://localhost:8092/api` (`frontend/vite.config.ts`).
- Server state is handled with React Query; lightweight UI state only lives in Zustand (`frontend/src/stores/chatStore.ts`, `themeStore.ts`).
- Existing frontend code reads `response.data.data`; note that `frontend/src/api/types.ts` still defines `ApiResponse.success`, but backend responses do not include that field.
- Do not assume every frontend helper matches a live endpoint: `providerApi.get` and `knowledgeApi.get` exist, but there are no matching `GET /api/v1/providers/{id}` or `GET /api/v1/knowledge-bases/{id}` handlers.
- `KnowledgeBaseRequest` expects nested `chunkStrategy` and `retrievalConfig`, while `KnowledgePage` create currently sends flat `chunkSize/chunkOverlap`; backend defaults currently hide that mismatch.

## Dev workflows
- Start infra from repo root: `docker-compose up -d` (PgVector on `5442`, Redis on `6379`).
- Build backend from `backend/`: `mvn clean install -DskipTests`; run services separately from `backend/server` and `backend/mcp-gateway` with `mvn spring-boot:run`.
- Run frontend from `frontend/`: `npm install` then `npm run dev`; `npm run build` is the only shipped frontend verification script.
- Backend tests live under `backend/**/src/test/java`; there is no frontend test script in `frontend/package.json`.

## Project-specific gotchas
- `backend/server/src/main/resources/application-dev.yml` defaults to `192.168.9.148` for Postgres and Redis, not Docker localhost; override with `APP_DATASOURCE_*`, `APP_REDIS_*`, and related env vars for local runs.
- Uploaded files are stored in repo-local `./uploads/<knowledgeBaseId>/...`; Git imports clone to a temp dir, then create normal `Document` rows.
- `Conversation` auto-titles itself from the first user message and deduplicates `mcpServerIds`; preserve that behavior in domain code, not controllers.
- Prompt templates are intentionally Chinese-first (`PromptTemplates`), even when the configured provider is international.

