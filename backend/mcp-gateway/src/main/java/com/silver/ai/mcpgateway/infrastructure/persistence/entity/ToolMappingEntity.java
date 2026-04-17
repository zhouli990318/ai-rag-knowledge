package com.silver.ai.mcpgateway.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tool_mapping", schema = "mcp_gateway")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ToolMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_source_id", nullable = false)
    private Long apiSourceId;

    @Column(name = "operation_id")
    private String operationId;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "tool_description", length = 2000)
    private String toolDescription;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    private String path;

    @Column(name = "parameter_schema", columnDefinition = "TEXT")
    private String parameterSchema;

    @Column(name = "response_schema", columnDefinition = "TEXT")
    private String responseSchema;

    @Column(name = "example_payload", columnDefinition = "TEXT")
    private String examplePayload;

    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
