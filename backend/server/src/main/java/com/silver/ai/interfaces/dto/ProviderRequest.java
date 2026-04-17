package com.silver.ai.interfaces.dto;

import com.silver.ai.domain.provider.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProviderRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotNull(message = "提供商类型不能为空")
    private ProviderType providerType;
    private String apiKey;
    private String baseUrl;
    private String defaultModel;
    private String embeddingModel;
    private Integer embeddingDimensions;
}
