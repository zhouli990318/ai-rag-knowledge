package com.silver.ai.domain.knowledge.port;

import com.silver.ai.domain.chat.model.PromptTemplates;

import java.util.Map;

/**
 * 提示渲染端口 — 领域层定义，基础设施层实现。
 * 用于将模板 + 变量渲染为最终提示文本。
 */
public interface PromptRendererPort {

    String render(PromptTemplates template);

    String render(PromptTemplates template, Map<String, ?> variables);
}
