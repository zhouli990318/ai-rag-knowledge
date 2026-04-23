package com.silver.ai.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("intent_node")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentNodeEntity {

    @Id
    private Long id;

    @Column("parent_id")
    private Long parentId;

    private String name;

    private String description;

    private int level;

    private String keywords;

    @Column("routing_advice")
    @Builder.Default
    private String routingAdvice = "RETRIEVAL";

    @Column("sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @Builder.Default
    private String status = "DRAFT";

    @Builder.Default
    private int version = 1;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
