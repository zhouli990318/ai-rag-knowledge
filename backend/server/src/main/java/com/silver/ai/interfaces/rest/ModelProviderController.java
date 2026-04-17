package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.ModelProviderAppService;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.interfaces.dto.ProviderRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderAppService providerAppService;

    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getTypes() {
        return ApiResponse.ok(providerAppService.getProviderTypes());
    }

    @GetMapping
    public ApiResponse<List<ModelProvider>> list() {
        return ApiResponse.ok(providerAppService.listProviders());
    }

    @PostMapping
    public ApiResponse<ModelProvider> create(@Valid @RequestBody ProviderRequest req) {
        ModelProvider provider = providerAppService.createProvider(
                req.getName(), req.getProviderType(), req.getApiKey(),
                req.getBaseUrl(), req.getDefaultModel(),
                req.getEmbeddingModel(), req.getEmbeddingDimensions());
        return ApiResponse.ok(provider);
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelProvider> update(@PathVariable Long id, @RequestBody ProviderRequest req) {
        ModelProvider provider = providerAppService.updateProvider(id,
                req.getName(), req.getApiKey(), req.getBaseUrl(),
                req.getDefaultModel(), req.getEmbeddingModel(), req.getEmbeddingDimensions());
        return ApiResponse.ok(provider);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerAppService.deleteProvider(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggle(@PathVariable Long id) {
        providerAppService.toggleProvider(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResponse<String> testConnection(@PathVariable Long id) {
        return ApiResponse.ok(providerAppService.testConnection(id));
    }
}
