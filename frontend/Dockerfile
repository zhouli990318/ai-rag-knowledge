# ============================================
# Stage 1: Build
# ============================================
FROM node:22-alpine AS builder

WORKDIR /build

# 先拷贝依赖描述文件，利用缓存
COPY package.json package-lock.json* ./

RUN npm ci --registry=https://registry.npmmirror.com

# 拷贝源码并构建
COPY . .

RUN npm run build

# ============================================
# Stage 2: Nginx Runtime
# ============================================
FROM nginx:alpine

LABEL maintainer="silver-ai"
LABEL description="AI RAG Platform - Frontend"
LABEL version="2.0.0"

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 拷贝自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段拷贝静态资源
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
