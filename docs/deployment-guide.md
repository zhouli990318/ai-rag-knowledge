# AI RAG Platform 部署操作手册

> 版本：2.0.0 | 更新日期：2026-05-09

---

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 架构概览](#2-架构概览)
- [3. 快速部署（一键启动）](#3-快速部署一键启动)
- [4. 环境变量配置说明](#4-环境变量配置说明)
- [5. 镜像构建详解](#5-镜像构建详解)
- [6. 部署方式](#6-部署方式)
  - [6.1 方式一：本地构建 + Docker Compose 直接部署](#61-方式一本地构建--docker-compose-直接部署)
  - [6.2 方式二：推送到镜像仓库（腾讯云/私有仓库）](#62-方式二推送到镜像仓库腾讯云私有仓库)
  - [6.3 方式三：离线部署（内网服务器）](#63-方式三离线部署内网服务器)
- [7. 部署脚本使用说明](#7-部署脚本使用说明)
- [8. 运维操作](#8-运维操作)
- [9. 服务架构与端口说明](#9-服务架构与端口说明)
- [10. 常见问题](#10-常见问题)

---

## 1. 环境要求

### 开发机（构建镜像）

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 25 | 后端编译 |
| Maven | 3.9+ | 后端依赖管理 |
| Node.js | 22+ | 前端构建（Docker 内完成，本地可不装） |
| Docker | 24+ | 镜像构建与运行 |
| Docker Compose | v2+ | 服务编排 |

### 目标服务器（运行）

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| Docker | 24+ | 容器运行时 |
| Docker Compose | v2+ | 服务编排 |
| 内存 | ≥ 4GB | 推荐 8GB+（含模型推理场景） |
| 磁盘 | ≥ 20GB | 镜像 + 数据库 + 上传文件 |

---

## 2. 架构概览

```
                    ┌─────────────────────────────────────────┐
                    │           Docker Network: ai-rag-net     │
                    │                                          │
  用户浏览器 ──80──►│  ┌──────────┐                            │
                    │  │ Frontend │ (Nginx)                    │
                    │  │ :80      │                            │
                    │  └────┬─────┘                            │
                    │       │                                  │
                    │  /api │        /mcp-api                  │
                    │       ▼              ▼                   │
                    │  ┌──────────┐  ┌──────────────┐         │
                    │  │ Server   │  │ MCP Gateway   │         │
                    │  │ :8091    │  │ :8092         │         │
                    │  └────┬─────┘  └───────┬──────┘         │
                    │       │                │                 │
                    │       ▼                ▼                 │
                    │  ┌──────────┐  ┌──────────┐             │
                    │  │ Postgres │  │  Redis   │             │
                    │  │ PgVector │  │  :6379   │             │
                    │  │ :5432    │  │          │             │
                    │  └──────────┘  └──────────┘             │
                    └─────────────────────────────────────────┘
```

| 服务 | 容器名 | 镜像 | 内部端口 | 对外端口 |
|------|--------|------|---------|---------|
| PostgreSQL + PgVector | ai-rag-postgres | pgvector/pgvector:pg16 | 5432 | 不暴露 |
| Redis | ai-rag-redis | redis:7-alpine | 6379 | 不暴露 |
| Server（主服务） | ai-rag-server | ai-rag/server:latest | 8091 | 不暴露 |
| MCP Gateway | ai-rag-mcp-gateway | ai-rag/mcp-gateway:latest | 8092 | 不暴露 |
| Frontend（Nginx） | ai-rag-frontend | ai-rag/frontend:latest | 80 | **80** |

> 所有后端服务仅在 Docker 内部网络可达，前端 Nginx 负责统一入口和 API 反向代理。

---

## 3. 快速部署（一键启动）

```bash
# 1. 克隆项目
git clone https://github.com/zhouli990318/ai-rag-knowledge.git
cd ai-rag-knowledge

# 2. 配置环境变量
cp .env.example .env
vim .env                    # 必须修改：数据库密码、AI API Key

# 3. 构建 + 启动
chmod +x deploy.sh
./deploy.sh build           # 本地 Maven 打包 + Docker 镜像构建
./deploy.sh up              # 启动全部 5 个服务

# 4. 检查状态
./deploy.sh status

# 5. 访问应用
# 浏览器打开 http://<服务器IP>
```

---

## 4. 环境变量配置说明

配置文件：项目根目录 `.env`（从 `.env.example` 复制）

### 数据库

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `POSTGRES_DB` | ai_rag_platform | 数据库名 |
| `POSTGRES_USER` | postgres | 数据库用户 |
| `POSTGRES_PASSWORD` | 123456 | **生产环境务必修改** |

### Redis

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `REDIS_PASSWORD` | （空） | 留空表示无密码，生产建议设置 |

### AI 模型 API

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `OPENAI_API_KEY` | placeholder | OpenAI / 兼容 API 密钥 |
| `OPENAI_BASE_URL` | https://api.openai.com | OpenAI 接口地址（可改为中转） |
| `OLLAMA_BASE_URL` | http://host.docker.internal:11434 | 本地 Ollama 地址 |
| `ANTHROPIC_API_KEY` | placeholder | Anthropic Claude 密钥 |
| `ZHIPUAI_API_KEY` | placeholder | 智谱 AI 密钥 |

> **Ollama 说明**：`host.docker.internal` 是 Docker 访问宿主机的特殊 hostname。如果 Ollama 运行在宿主机上，保持默认即可；如果运行在其他服务器上，改为该服务器 IP。

### 嵌入模型

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `APP_DEFAULT_EMBEDDING_BASE_URL` | http://host.docker.internal:11434 | 嵌入模型服务地址 |
| `APP_DEFAULT_EMBEDDING_MODEL` | bge-m3 | 嵌入模型名称 |
| `APP_DEFAULT_EMBEDDING_DIMENSIONS` | 1024 | 嵌入向量维度 |

### 应用

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | dev | Spring 激活配置 |
| `APP_CRYPTO_KEY` | SpringAiRagPlatform2024SecretKey | AES-256-GCM 加密密钥（**生产务必修改**） |
| `SERVER_JAVA_OPTS` | -XX:+UseZGC -XX:MaxRAMPercentage=75.0 | Server JVM 参数 |
| `GATEWAY_JAVA_OPTS` | -XX:+UseZGC -XX:MaxRAMPercentage=75.0 | Gateway JVM 参数 |
| `FRONTEND_PORT` | 80 | 前端对外端口 |

### 镜像仓库

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `REGISTRY` | （空） | 镜像仓库地址，留空为本地 |
| `PROJECT` | ai-rag | 镜像项目名 |
| `TAG` | latest | 镜像标签 |

---

## 5. 镜像构建详解

项目包含 3 个 Dockerfile：

### 后端镜像（Server / MCP Gateway）

```
backend/server/Dockerfile       → ai-rag/server:latest
backend/mcp-gateway/Dockerfile  → ai-rag/mcp-gateway:latest
```

**构建流程**：本地 `mvn package` 打出 JAR → Dockerfile 仅负责拷贝 JAR 到 `eclipse-temurin:25-jre` 运行镜像。

- 基础镜像：`eclipse-temurin:25-jre`（约 200MB）
- 非 root 用户 `appuser` 运行
- 默认 JVM：ZGC + 75% 最大内存
- 时区：`Asia/Shanghai`

### 前端镜像

```
frontend/Dockerfile             → ai-rag/frontend:latest
```

**构建流程**：Docker 内多阶段构建。

- Stage 1：`node:22-alpine` 中 `npm ci` + `npm run build` 生成静态文件
- Stage 2：`nginx:alpine` 托管静态文件 + 反向代理后端 API

Nginx 配置要点（`frontend/nginx.conf`）：

| 路径 | 转发目标 | 说明 |
|------|---------|------|
| `/api/*` | `http://server:8091/api/*` | 主服务 API（含 SSE 流式聊天） |
| `/mcp-api/*` | `http://mcp-gateway:8092/api/*` | MCP 网关 API |
| `/assets/*` | 本地静态文件 | 1 年缓存（Vite hash） |
| `/*` | `index.html` | SPA 路由 fallback |

---

## 6. 部署方式

### 6.1 方式一：本地构建 + Docker Compose 直接部署

适用于：开发测试、单台服务器部署。

```bash
# 在项目根目录执行
cp .env.example .env
vim .env                        # 修改配置

./deploy.sh build               # Maven 打包 + Docker 构建
./deploy.sh up                  # 启动
```

### 6.2 方式二：推送到镜像仓库（腾讯云/私有仓库）

适用于：多服务器部署、CI/CD 流水线。

**步骤一：开发机构建并推送**

```bash
# .env 配置镜像仓库
REGISTRY=ccr.ccs.tencentyun.com/your-namespace
TAG=2.0.0

# 登录仓库
docker login ccr.ccs.tencentyun.com -u <用户名>

# 构建并推送
./deploy.sh build
./deploy.sh push
```

推送后的镜像：
```
ccr.ccs.tencentyun.com/your-namespace/ai-rag/server:2.0.0
ccr.ccs.tencentyun.com/your-namespace/ai-rag/mcp-gateway:2.0.0
ccr.ccs.tencentyun.com/your-namespace/ai-rag/frontend:2.0.0
```

**步骤二：目标服务器拉取并启动**

```bash
# 将以下文件传到服务器
#   - docker-compose.prod.yml
#   - .env（已配置 REGISTRY/TAG）
#   - init-db.sql

docker login ccr.ccs.tencentyun.com -u <用户名>
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### 6.3 方式三：离线部署（内网服务器）

适用于：无外网环境、内网隔离部署。

**步骤一：开发机导出镜像**

```bash
./deploy.sh build               # 构建镜像
./deploy.sh save                # 导出到 ./images/ 目录
```

产出文件：
```
images/
├── ai-rag-server-latest.tar        (~250MB)
├── ai-rag-mcp-gateway-latest.tar   (~220MB)
└── ai-rag-frontend-latest.tar      (~30MB)
```

还需要导出基础设施镜像（首次部署时）：
```bash
docker pull pgvector/pgvector:pg16
docker pull redis:7-alpine
docker save -o images/pgvector-pg16.tar pgvector/pgvector:pg16
docker save -o images/redis-7-alpine.tar redis:7-alpine
```

**步骤二：传输到目标服务器**

```bash
# SCP / USB / 内网传输
scp -r images/ user@target-server:/opt/ai-rag/
scp docker-compose.prod.yml .env init-db.sql user@target-server:/opt/ai-rag/
```

**步骤三：目标服务器加载并启动**

```bash
cd /opt/ai-rag

# 加载所有镜像
for f in images/*.tar; do docker load -i "$f"; done

# 启动
docker compose -f docker-compose.prod.yml up -d
```

---

## 7. 部署脚本使用说明

```bash
./deploy.sh <command>
```

| 命令 | 说明 | 示例 |
|------|------|------|
| `build` | 本地 Maven 打包 + Docker 镜像构建 | `./deploy.sh build` |
| `up` | 启动全部服务 | `./deploy.sh up` |
| `down` | 停止并移除全部容器 | `./deploy.sh down` |
| `restart` | 重启全部服务 | `./deploy.sh restart` |
| `logs` | 查看日志（可指定服务名） | `./deploy.sh logs server` |
| `push` | 推送镜像到远程仓库 | `./deploy.sh push` |
| `save` | 导出镜像为 tar（离线部署） | `./deploy.sh save` |
| `status` | 查看服务运行状态 | `./deploy.sh status` |

---

## 8. 运维操作

### 查看服务状态

```bash
./deploy.sh status
# 或
docker compose -f docker-compose.prod.yml ps
```

### 查看日志

```bash
# 全部日志
./deploy.sh logs

# 指定服务
./deploy.sh logs server
./deploy.sh logs mcp-gateway
./deploy.sh logs frontend
./deploy.sh logs postgres

# 最近 100 行
docker compose -f docker-compose.prod.yml logs --tail 100 server
```

### 重启单个服务

```bash
docker compose -f docker-compose.prod.yml restart server
docker compose -f docker-compose.prod.yml restart mcp-gateway
```

### 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并重启
./deploy.sh build
./deploy.sh down
./deploy.sh up
```

### 仅更新某个服务

```bash
# 示例：只重新构建和更新 server
cd backend && mvn clean package -pl server -am -DskipTests && cd ..
docker compose -f docker-compose.prod.yml build server
docker compose -f docker-compose.prod.yml up -d server
```

### 数据备份

```bash
# 备份 PostgreSQL
docker exec ai-rag-postgres pg_dump -U postgres ai_rag_platform > backup_$(date +%Y%m%d).sql

# 备份上传文件
docker cp ai-rag-server:/app/uploads ./uploads_backup/

# 恢复数据库
cat backup_20260509.sql | docker exec -i ai-rag-postgres psql -U postgres ai_rag_platform
```

### 查看资源占用

```bash
docker stats ai-rag-server ai-rag-mcp-gateway ai-rag-frontend ai-rag-postgres ai-rag-redis
```

---

## 9. 服务架构与端口说明

### Docker 内部网络

所有服务通过 `ai-rag-net` 桥接网络通信，使用容器名作为 hostname：

| 从 | 到 | 地址 |
|----|-----|------|
| Nginx | Server | `http://server:8091` |
| Nginx | MCP Gateway | `http://mcp-gateway:8092` |
| Server | PostgreSQL | `postgresql://postgres:5432` |
| Server | Redis | `redis:6379` |
| Server | MCP Gateway | `http://mcp-gateway:8092/api/v1/mcp` |
| MCP Gateway | PostgreSQL | `postgresql://postgres:5432` |
| MCP Gateway | Redis | `redis:6379` |
| Server | Ollama（宿主机） | `http://host.docker.internal:11434` |

### 数据持久化

| Docker Volume | 挂载点 | 说明 |
|--------------|--------|------|
| `pgdata` | /var/lib/postgresql/data | 数据库数据 |
| `redisdata` | /data | Redis 持久化数据 |
| `uploads` | /app/uploads | 用户上传的知识库文件 |

---

## 10. 常见问题

### Q1：服务启动后访问白屏或 502

**原因**：后端服务尚未完全启动。Spring Boot + Flyway 数据库迁移需要时间。

**解决**：
```bash
# 等待健康检查通过
./deploy.sh status

# 查看 server 启动日志
./deploy.sh logs server
```

### Q2：前端可以访问但 API 报错 "502 Bad Gateway"

**原因**：Nginx 无法连接后端服务。

**排查**：
```bash
# 确认后端容器在运行
docker ps | grep ai-rag

# 进入 frontend 容器测试连通性
docker exec ai-rag-frontend wget -qO- http://server:8091/actuator/health
```

### Q3：数据库连接失败

**原因**：Postgres 未就绪或密码不匹配。

**排查**：
```bash
# 检查 postgres 健康状态
docker inspect ai-rag-postgres --format='{{.State.Health.Status}}'

# 手动连接测试
docker exec -it ai-rag-postgres psql -U postgres -d ai_rag_platform
```

### Q4：Ollama 模型调用失败

**原因**：`host.docker.internal` 在 Linux 服务器上可能不支持。

**解决**：
```bash
# 方式 1：使用宿主机 IP
OLLAMA_BASE_URL=http://192.168.1.100:11434

# 方式 2：Docker Compose 中添加 extra_hosts（已在容器内自动支持）
# 方式 3：将 Ollama 也放入 Docker 网络
```

### Q5：磁盘空间不足

```bash
# 清理无用镜像和构建缓存
docker system prune -a

# 查看各容器磁盘占用
docker system df
```

### Q6：如何修改前端访问端口

```bash
# .env 中修改
FRONTEND_PORT=8080

# 重启
./deploy.sh restart
# 访问 http://<IP>:8080
```

### Q7：如何启用 HTTPS

在 Nginx 前加一层反向代理（推荐 Caddy 或外部 Nginx），或修改 `frontend/nginx.conf` 添加 SSL 证书配置：

```bash
# 方式 1（推荐）：使用 Caddy 自动 HTTPS
# 安装 Caddy 后创建 Caddyfile:
your-domain.com {
    reverse_proxy localhost:80
}

# 方式 2：挂载证书到 Nginx 容器
# 在 docker-compose.prod.yml 的 frontend 服务添加:
volumes:
  - ./certs/fullchain.pem:/etc/nginx/ssl/fullchain.pem:ro
  - ./certs/privkey.pem:/etc/nginx/ssl/privkey.pem:ro
```
