package com.silver.ai.domain.chat.model;

/**
 * 对话编排阶段枚举 — 定义主链路中每个可观测阶段。
 * 用于链路追踪 Trace 记录和配置门控。
 */
public enum OrchestrationStage {

    /** 问题重写与上下文补全 */
    REWRITE,
    /** 意图识别与分类 */
    INTENT,
    /** 知识检索（多路并行） */
    RETRIEVAL,
    /** 结果重排序与融合 */
    RERANK,
    /** MCP 工具调用 */
    TOOL,
    /** 模型生成 */
    GENERATION,
    /** 持久化 */
    PERSIST
}
