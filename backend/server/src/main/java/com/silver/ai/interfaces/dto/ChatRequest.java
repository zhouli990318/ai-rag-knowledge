package com.silver.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private Long conversationId;
    @NotNull(message = "providerId 不能为空")
    private Long providerId;
    @Size(max = 100, message = "model 长度不能超过 100")
    private String model;
    @Size(max = 4000, message = "systemPrompt 长度不能超过 4000")
    private String systemPrompt;
    @NotBlank(message = "消息不能为空")
    @Size(max = 32000, message = "消息长度不能超过 32000")
    private String message;
    private Long knowledgeBaseId;
    @Size(max = 20, message = "mcpServerIds 数量不能超过 20")
    private List<Long> mcpServerIds;
    /** 工具模式：OFF | AUTO | SPECIFIC（默认 AUTO） */
    private String toolMode;
}
