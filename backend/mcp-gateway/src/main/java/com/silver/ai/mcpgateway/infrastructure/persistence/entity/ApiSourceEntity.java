package com.silver.ai.mcpgateway.infrastructure.persistence.entity;

import com.silver.ai.mcpgateway.domain.model.AuthType;
import com.silver.ai.mcpgateway.domain.model.ProtocolType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_source", schema = "mcp_gateway")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_type")
    @Builder.Default
    private ProtocolType protocolType = ProtocolType.HTTP;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "openapi_spec", columnDefinition = "TEXT")
    private String openApiSpec;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    @Builder.Default
    private AuthType authType = AuthType.NONE;

    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
