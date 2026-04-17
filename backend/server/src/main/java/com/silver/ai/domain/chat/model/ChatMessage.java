package com.silver.ai.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private Long id;
    private Long conversationId;
    private MessageRole role;
    private String content;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
