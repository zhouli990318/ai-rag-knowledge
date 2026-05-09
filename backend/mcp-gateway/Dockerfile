FROM eclipse-temurin:25-jre

LABEL maintainer="silver-ai"
LABEL description="AI RAG Platform - MCP Gateway"
LABEL version="2.0.0"

WORKDIR /app

# 安装 curl（健康检查需要）+ 设置时区
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl tzdata && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 拷贝本地构建好的 JAR
COPY mcp-gateway/target/mcp-gateway-*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof" \
    TZ=Asia/Shanghai

EXPOSE 8092

VOLUME ["/app/logs"]

HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=60s \
    CMD curl -sf http://localhost:8092/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]