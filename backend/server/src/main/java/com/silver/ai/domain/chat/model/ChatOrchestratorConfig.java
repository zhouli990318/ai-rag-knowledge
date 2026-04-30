package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.silver.ai.domain.chat.model.PromptTemplates.SUGGEST_FOLLOW_UP;

/**
 * 对话编排运行时配置 — 值对象。
 * 由 application.yml 绑定，控制各阶段行为。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatOrchestratorConfig {

    // ── 意图识别 ──
    /** 意图分类置信度阈值，低于此值触发澄清 */
    @Builder.Default
    private double intentConfidenceThreshold = 0.6;

    /** 是否启用意图识别 */
    @Builder.Default
    private boolean intentEnabled = true;

    // ── 问题重写 ──
    /** 是否启用问题重写 */
    @Builder.Default
    private boolean rewriteEnabled = true;

    /** 重写参考的历史轮次数 */
    @Builder.Default
    private int rewriteContextRounds = 5;

    // ── 检索 ──
    /** 是否启用多路检索（false 则退化为单路向量检索） */
    @Builder.Default
    private boolean multiPathRetrievalEnabled = true;

    /** 重排序后保留的最终文档数 */
    @Builder.Default
    private int rerankTopK = 5;

    /** 多路检索并行子查询超时秒数 */
    @Builder.Default
    private int retrievalTimeoutSeconds = 10;

    /** 去重指纹截取长度 */
    @Builder.Default
    private int deduplicatePrefixLength = 200;

    // ── 记忆管理 ──
    /** 保留近 N 轮完整对话 */
    @Builder.Default
    private int memoryFullRounds = 10;

    /** 上下文字符限制 */
    @Builder.Default
    private int memoryMaxChars = 12000;

    /** 触发摘要压缩的消息条数阈值 */
    @Builder.Default
    private int memorySummaryThreshold = 20;

    // ── 模型路由 ──
    /** 是否启用自动降级 */
    @Builder.Default
    private boolean modelFallbackEnabled = true;

    /** 最大重试次数 */
    @Builder.Default
    private int modelMaxRetries = 2;

    /** 健康检查间隔分钟数 */
    @Builder.Default
    private int healthCheckIntervalMinutes = 5;

    // ── 工具调用 ──
    /** 工具自动执行的置信度阈值（低于此值先确认） */
    @Builder.Default
    private double toolAutoExecuteThreshold = 0.8;

    /** 是否启用工具语义检索（true: 仅注入相关工具; false: 全量注入） */
    @Builder.Default
    private boolean toolSemanticRetrievalEnabled = true;

    /** 语义检索召回工具数量 */
    @Builder.Default
    private int toolRetrievalTopK = 10;

    /** 语义检索最低相似度阈值 */
    @Builder.Default
    private double toolRetrievalThreshold = 0.3;

    /** 工具调用失败是否自动回退到检索/直答 */
    @Builder.Default
    private boolean toolFallbackEnabled = true;

    // ── 追踪 ──
    /** 追踪采样率 0.0~1.0 */
    @Builder.Default
    private double traceSampleRate = 1.0;

    /** 是否启用全链路追踪 */
    @Builder.Default
    private boolean traceEnabled = true;

    // ── 辅助任务 ──
    /** 辅助任务（摘要压缩、推荐问题）使用的提供商 ID，为 null 则使用会话自身的提供商 */
    private Long auxiliaryProviderId;

    /** 推荐问题提示词模板，{conversation} 为占位符 */
    @Builder.Default
    private String suggestionPrompt = SUGGEST_FOLLOW_UP.getTemplate();
}
