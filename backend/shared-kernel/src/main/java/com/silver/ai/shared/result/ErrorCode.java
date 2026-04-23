package com.silver.ai.shared.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    INTERNAL_ERROR(50000, "系统内部错误"),
    INVALID_PARAMETER(40000, "参数校验失败"),
    RESOURCE_NOT_FOUND(40400, "资源不存在"),
    DUPLICATE_RESOURCE(40900, "资源已存在"),

    // AI 提供商
    PROVIDER_NOT_FOUND(41001, "AI提供商不存在"),
    PROVIDER_DISABLED(41002, "AI提供商已禁用"),
    PROVIDER_CONNECTION_FAILED(41003, "AI提供商连接失败"),
    PROVIDER_MODEL_NOT_AVAILABLE(41004, "模型不可用"),

    // 知识库
    KNOWLEDGE_BASE_NOT_FOUND(42001, "知识库不存在"),
    DOCUMENT_NOT_FOUND(42002, "文档不存在"),
    DOCUMENT_PROCESSING_FAILED(42003, "文档处理失败"),
    DOCUMENT_PARSE_FAILED(42004, "文档解析失败"),
    UNSUPPORTED_FILE_TYPE(42005, "不支持的文件类型"),
    VECTOR_STORE_ERROR(42006, "向量存储错误"),

    // 对话
    CONVERSATION_NOT_FOUND(43001, "对话不存在"),
    CHAT_STREAM_ERROR(43002, "流式对话异常"),

    // MCP
    MCP_SOURCE_NOT_FOUND(44001, "MCP API源不存在"),
    MCP_PARSE_FAILED(44002, "OpenAPI规范解析失败"),
    MCP_TOOL_INVOCATION_FAILED(44003, "MCP工具调用失败"),
    MCP_TOOL_NOT_FOUND(44004, "MCP工具不存在"),

    // 意图识别
    INTENT_CLASSIFICATION_FAILED(45001, "意图识别失败"),
    INTENT_CLARIFICATION_NEEDED(45002, "需要进一步澄清"),

    // 模型路由
    PROVIDER_NOT_AVAILABLE(41005, "无可用AI提供商"),
    MODEL_HEALTH_CHECK_FAILED(41006, "模型健康检查失败"),

    // ETL
    ETL_TASK_NOT_FOUND(46001, "入库任务不存在"),
    ETL_STAGE_FAILED(46002, "入库阶段执行失败"),

    // 追踪
    TRACE_NOT_FOUND(47001, "链路追踪记录不存在");

    private final int code;
    private final String message;
}
