#!/usr/bin/env bash
set -euo pipefail

# ============================================
# AI RAG Platform 部署脚本
# 用法: ./deploy.sh [command]
#   build   - 构建所有镜像
#   up      - 启动全部服务
#   down    - 停止全部服务
#   restart - 重启全部服务
#   logs    - 查看日志（可追加服务名）
#   push    - 推送镜像到仓库
#   save    - 导出镜像为 tar（离线部署）
#   status  - 查看服务状态
# ============================================

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env"

# 加载 .env
if [ -f "$ENV_FILE" ]; then
    set -a; source "$ENV_FILE"; set +a
fi

REGISTRY="${REGISTRY:-}"
PROJECT="${PROJECT:-ai-rag}"
TAG="${TAG:-latest}"

# 镜像全名
image_name() {
    local service=$1
    if [ -n "$REGISTRY" ]; then
        echo "${REGISTRY}/${PROJECT}/${service}:${TAG}"
    else
        echo "${PROJECT}/${service}:${TAG}"
    fi
}

SERVICES=("server" "mcp-gateway" "frontend")

case "${1:-help}" in
    build)
        echo "==> 本地构建 JAR..."
        cd backend && mvn clean package -DskipTests -q && cd ..
        echo "==> 构建 Docker 镜像..."
        docker compose -f "$COMPOSE_FILE" build
        echo "==> 构建完成"
        echo "镜像列表:"
        for svc in "${SERVICES[@]}"; do
            echo "  - $(image_name "$svc")"
        done
        ;;

    up)
        echo "==> 启动全部服务..."
        docker compose -f "$COMPOSE_FILE" up -d
        echo "==> 启动完成，查看状态: ./deploy.sh status"
        ;;

    down)
        echo "==> 停止全部服务..."
        docker compose -f "$COMPOSE_FILE" down
        echo "==> 已停止"
        ;;

    restart)
        echo "==> 重启全部服务..."
        docker compose -f "$COMPOSE_FILE" down
        docker compose -f "$COMPOSE_FILE" up -d
        echo "==> 重启完成"
        ;;

    logs)
        shift
        docker compose -f "$COMPOSE_FILE" logs -f "$@"
        ;;

    push)
        echo "==> 推送镜像到仓库..."
        if [ -z "$REGISTRY" ]; then
            echo "错误: REGISTRY 未设置，请在 .env 中配置镜像仓库地址"
            echo "示例: REGISTRY=ccr.ccs.tencentyun.com/your-namespace"
            exit 1
        fi
        for svc in "${SERVICES[@]}"; do
            local_image="${PROJECT}/${svc}:${TAG}"
            remote_image="$(image_name "$svc")"
            echo "  推送 ${remote_image}..."
            docker tag "$local_image" "$remote_image" 2>/dev/null || true
            docker push "$remote_image"
        done
        echo "==> 推送完成"
        ;;

    save)
        OUTPUT_DIR="${2:-./images}"
        mkdir -p "$OUTPUT_DIR"
        echo "==> 导出镜像到 ${OUTPUT_DIR}/..."
        for svc in "${SERVICES[@]}"; do
            img="$(image_name "$svc")"
            file="${OUTPUT_DIR}/${PROJECT}-${svc}-${TAG}.tar"
            echo "  导出 ${img} -> ${file}"
            docker save -o "$file" "$img"
        done
        echo "==> 导出完成。在目标服务器执行:"
        echo "    docker load -i ${OUTPUT_DIR}/<镜像文件>.tar"
        echo "    docker compose -f ${COMPOSE_FILE} up -d"
        ;;

    status)
        docker compose -f "$COMPOSE_FILE" ps
        ;;

    *)
        echo "AI RAG Platform 部署脚本"
        echo ""
        echo "用法: $0 <command>"
        echo ""
        echo "命令:"
        echo "  build     构建所有 Docker 镜像"
        echo "  up        启动全部服务（构建+启动: build 后 up）"
        echo "  down      停止并移除全部容器"
        echo "  restart   重启全部服务"
        echo "  logs      查看日志（可追加服务名，如: logs server）"
        echo "  push      推送镜像到远程仓库（需配置 REGISTRY）"
        echo "  save      导出镜像为 tar 文件（离线部署用）"
        echo "  status    查看服务运行状态"
        echo ""
        echo "快速开始:"
        echo "  1. cp .env.example .env     # 复制并修改配置"
        echo "  2. ./deploy.sh build        # 构建镜像"
        echo "  3. ./deploy.sh up           # 启动服务"
        echo "  4. 访问 http://localhost     # 打开应用"
        ;;
esac
