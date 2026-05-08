package com.silver.ai.domain.knowledge.model;

import java.util.Map;

/**
 * 向量文档 — 用于向量存储端口的领域值对象，替代基础设施层的 Spring AI Document。
 */
public record VectorDocument(
        String content,
        Map<String, Object> metadata
) {
}
