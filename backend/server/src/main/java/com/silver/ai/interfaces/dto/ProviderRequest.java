package com.silver.ai.interfaces.dto;

import com.silver.ai.domain.provider.model.ProviderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProviderRequest {
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过 100")
    private String name;
    @NotNull(message = "提供商类型不能为空")
    private ProviderType providerType;
    @Size(max = 500, message = "apiKey 长度不能超过 500")
    private String apiKey;
    @Size(max = 500, message = "baseUrl 长度不能超过 500")
    private String baseUrl;
    @Size(max = 100, message = "defaultModel 长度不能超过 100")
    private String defaultModel;
    @Size(max = 100, message = "embeddingModel 长度不能超过 100")
    private String embeddingModel;
    @Min(value = 1, message = "embeddingDimensions 最小为 1")
    private Integer embeddingDimensions;
}
