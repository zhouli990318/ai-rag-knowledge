package com.silver.ai.mcpgateway.interfaces.dto;

import com.silver.ai.mcpgateway.domain.model.AuthType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiSourceRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String description;
    @NotBlank(message = "baseUrl不能为空")
    private String baseUrl;
    private AuthType authType;
    private String authConfig;
    private String openApiSpec;
    private String openApiUrl;
}
