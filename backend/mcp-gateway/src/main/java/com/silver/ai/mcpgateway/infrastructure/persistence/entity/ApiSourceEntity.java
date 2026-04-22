package com.silver.ai.mcpgateway.infrastructure.persistence.entity;

import com.silver.ai.mcpgateway.domain.model.AuthType;
import com.silver.ai.mcpgateway.domain.model.ProtocolType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("mcp_gateway.api_source")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiSourceEntity {

    @Id
    private Long id;

    private String name;

    private String description;

    @Column("protocol_type")
    @Builder.Default
    private ProtocolType protocolType = ProtocolType.HTTP;

    @Column("base_url")
    private String baseUrl;

    @Column("openapi_spec")
    private String openApiSpec;

    @Column("auth_type")
    @Builder.Default
    private AuthType authType = AuthType.NONE;

    @Column("auth_config")
    private String authConfig;

    @Builder.Default
    private boolean active = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
