# Spring AI RAG 应用

这是一个基于Spring AI和Angular的RAG（检索增强生成）应用，支持多种大语言模型（LLM）和知识库管理功能。

## 项目结构

项目采用DDD（领域驱动设计）架构，分为以下模块：

- **api**: 包含REST API控制器和请求/响应模型
- **application**: 包含应用服务和用例实现
- **domain**: 包含领域模型、实体和业务逻辑
- **infrastructure**: 包含基础设施代码，如存储库实现和外部服务集成
- **shared**: 包含共享模型和工具类
- **angular-ui**: 包含Angular前端应用

## 功能特性

- 支持多种LLM提供商（OpenAI、Ollama等）
- 知识库管理（上传文件、创建标签）
- Git仓库分析和导入
- 基于知识库的RAG对话
- 流式响应支持

## 技术栈

### 后端

- Spring Boot 3.4.5
- Spring AI 1.0.0
- Java 21
- Maven
- Redisson（Redis客户端）
- JGit（Git操作）

### 前端

- Angular 17
- TypeScript
- SCSS
- Angular Router
- HttpClient with Fetch
- Markdown渲染

## 开发环境设置

### 前提条件

- JDK 21
- Maven 3.8+
- Node.js 18+
- npm 9+

### 构建和运行

1. 克隆仓库

```bash
git clone <repository-url>
cd SpringAiRAG
```

2. 构建项目

```bash
mvn clean install
```

3. 运行应用

```bash
mvn spring-boot:run -pl application
```

应用将在 http://localhost:8090 上运行。

### 单独开发前端

如果你想单独开发前端，可以：

```bash
cd angular-ui
npm install
npm start
```

前端开发服务器将在 http://localhost:4200 上运行，并自动代理API请求到后端。

## 使用指南

### AI聊天

1. 访问首页的"AI聊天"选项卡
2. 选择LLM提供商和模型
3. 开启或关闭RAG功能
4. 如果开启RAG，选择相关知识库标签
5. 输入问题并发送

### 知识库管理

1. 访问"知识库管理"选项卡
2. 创建新标签或选择现有标签
3. 上传文件到选定标签
4. 或者提供Git仓库URL、用户名和令牌进行分析

## 配置

应用配置位于`application/src/main/resources/application.yml`文件中。

## 许可证

[MIT](LICENSE)