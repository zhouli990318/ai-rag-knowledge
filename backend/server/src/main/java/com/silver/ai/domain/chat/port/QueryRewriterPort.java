package com.silver.ai.domain.chat.port;

import java.util.List;

/**
 * 问题重写端口 — 领域层纯接口，由 infrastructure 提供实现。
 */
public interface QueryRewriterPort {

    /**
     * 基于上下文重写用户查询（指代消解、上下文补全）。
     *
     * @param originalQuery     原始查询
     * @param conversationContext 近几轮对话
     * @return 重写后的查询
     */
    String rewrite(String originalQuery, List<String> conversationContext);

    /**
     * 将复杂问题拆分为可独立检索的子问题。
     *
     * @param query 原始或已重写的查询
     * @return 子问题列表（至少包含原问题自身）
     */
    List<String> decompose(String query);
}
