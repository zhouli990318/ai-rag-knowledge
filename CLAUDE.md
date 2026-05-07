# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Infrastructure
```bash
docker-compose up -d    # PgVector on 5442, Redis on 6379
```

### Backend (from `backend/`)
```bash
mvn clean install -DskipTests   # Build all modules
mvn test                         # Run all tests (27 test classes, JUnit 5 + Mockito)
```

Run individual services:
```bash
cd backend/server && mvn spring-boot:run       # Port 8091
cd backend/mcp-gateway && mvn spring-boot:run  # Port 8092
```

Run a single test class:
```bash
mvn -pl server test -Dtest=ChatAppServiceTest
```

### Frontend (from `frontend/`)
```bash
npm install
npm run dev     # Dev server on port 5173
npm run build   # tsc -b && vite build (the only verification script — no tests)
```

## Environment Overrides

`backend/server/src/main/resources/application-dev.yml` defaults to `192.168.9.148` for Postgres/Redis. Override for local runs:
```bash
export APP_DATASOURCE_URL="jdbc:postgresql://localhost:5442/ai_rag_platform"
export APP_DATASOURCE_USERNAME="postgres"
export APP_DATASOURCE_PASSWORD="123456"
export APP_REDIS_HOST="localhost"
export APP_REDIS_PORT="6379"
```

## Architecture

DDD + Hexagonal (Ports & Adapters) monorepo. Three Maven modules and one Vite frontend.

### Modules

- **`backend/shared-kernel`** — `ApiResponse`, `ErrorCode` (22 codes), `BusinessException`, `GlobalExceptionHandler`, `CryptoUtil` (AES-256-GCM). Shared by both services.
- **`backend/server`** (port 8091) — Main app: chat, providers, knowledge bases, vector search. Package: `com.silver.ai`.
- **`backend/mcp-gateway`** (port 8092) — Reactive WebFlux service bridging OpenAPI specs to MCP protocol (SSE + Streamable HTTP). Own `mcp_gateway` schema. Package: `com.silver.ai.mcpgateway`.
- **`frontend/`** — React 19 + Vite 6 + MUI 6 + Zustand + TanStack Query. "Ink" (水墨) design system with glassmorphism.

### Hexagonal Layer Convention

Each bounded context follows: `domain/**` (models + ports) → `application/service/**` (orchestration) → `infrastructure/**` (adapters/config) → `interfaces/**` (controllers/DTOs).

Domain ports are interfaces (e.g., `VectorStorePort`, `ChatModelPort`). Infrastructure provides adapters (e.g., `PgVectorStoreAdapter`, `ChatModelAdapter`). Repository pattern: `KnowledgeBaseRepository` (port) → `KnowledgeBaseRepositoryAdapter` → `R2dbcKnowledgeBaseRepository`.

### Bounded Contexts (server)

| Context | Key Domain Services | Key Ports |
|---------|-------------------|-----------|
| **chat** | `ChatOrchestrator`, `IntentDecisionDomainService`, `ToolRoutingDomainService` | `ChatModelPort`, `McpToolPort`, `IntentClassifierPort` |
| **knowledge** | `DocumentProcessingDomainService`, `RetrievalDomainService`, `MultiPathRetrievalDomainService` | `VectorStorePort`, `DocumentParserPort`, `TextSplitterPort` |
| **provider** | `ModelRoutingDomainService` | `ChatModelPort`, `EmbeddingPort`, `ModelSelectionPort` |

### Key Data Flows

- **Chat**: `ChatController` → `ChatAppService` → `ChatOrchestrator` (intent → retrieval → tool routing) → `ChatModelPort`
- **RAG ingest**: upload/git → `KnowledgeBaseAppService` → `DocumentProcessingDomainService` (parse → split → vectorize)
- **MCP**: frontend `/mcp-api` → `McpGatewayController` → `McpGatewayAppService` → `ToolInvocationDomainService`; server consumes tools via `McpToolGatewayClient` using `app.mcp-gateway.base-url`

### Database

- PostgreSQL 16 + PgVector. Flyway migrations in `backend/server/src/main/resources/db/migration/` (V1–V8) and `backend/mcp-gateway/src/main/resources/db/migration/` (V1–V3, `mcp_gateway` schema).
- Data access is **R2DBC** (reactive), not JPA. Entities in `infrastructure/persistence/entity/`, R2DBC repos in `infrastructure/persistence/r2dbc/`.
- Vector metadata keys: `knowledge_base_id`, `document_id`, `file_name`, `file_type`. Default embedding: bge-m3 (1024-dim, HNSW, cosine).

### Frontend Conventions

- Vite proxy: `/api` → `:8091`, `/mcp-api` → `:8092/api` (configured in `vite.config.ts`, not hardcoded in API calls).
- Two Axios clients in `api/client.ts`: `api` (main backend, no baseURL) and `mcpApi` (gateway, `baseURL: '/mcp-api'`). All modules unwrap `ApiResponse.data` in `.then()`.
- Streaming chat uses native `fetch()` for SSE, parsed in `chatStore.ts`. `fixIncompleteMarkdown()` repairs unclosed syntax during streaming.
- State: React Query for server state; Zustand for UI state (`chatStore`, `chatConfigStore` persisted to localStorage, `themeStore`).
- `chatConfigStore` persists provider, KB, tool mode, and MCP server selections across sessions.
- Components use `memo()`, `useCallback`, Framer Motion animations. Dialogs are local to their parent page.
- UI primitives live in `components/ink/` (InkCard, InkButton, InkBadge, etc.) with tokens from `theme/ThemeProvider.tsx`.

## Important Gotchas

- **No frontend tests exist.** `npm run build` is the only verification.
- **Do not assume every frontend API helper has a matching backend endpoint.** For example, `providerApi.get` and `knowledgeApi.get` exist in the frontend but have no corresponding `GET /{id}` handlers server-side.
- **`KnowledgeBaseRequest` expects nested `chunkStrategy` and `retrievalConfig`**, but `KnowledgePage` create currently sends flat `chunkSize/chunkOverlap`. Backend defaults hide the mismatch.
- **Prompt templates are Chinese-first** (`PromptTemplates`), even when the configured provider is international.
- **`Conversation` auto-titles from the first user message** and deduplicates `mcpServerIds`. Preserve that behavior in domain code, not controllers.
- **Uploads are stored in repo-local `./uploads/<knowledgeBaseId>/...`**; Git imports clone to a temp dir then create normal `Document` rows.
- **The two services use separate schemas** (`ai_rag_platform` default, `mcp_gateway` for the gateway). Flyway is configured independently per service.
