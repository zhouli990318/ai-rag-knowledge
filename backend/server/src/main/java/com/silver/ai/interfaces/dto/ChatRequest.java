package com.silver.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private Long conversationId;
    @NotNull(message = "providerId 不能为空")
    private Long providerId;
    private String model;
    private String systemPrompt;
    @NotBlank(message = "消息不能为空")
    private String message;
    private Long knowledgeBaseId;
    private List<Long> mcpServerIds;
}
