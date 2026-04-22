package com.silver.ai.infrastructure.persistence.entity;

import com.silver.ai.domain.chat.model.MessageRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("chat_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {

    @Id
    private Long id;

    @Column("conversation_id")
    private Long conversationId;

    private MessageRole role;

    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;
}
