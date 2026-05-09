package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.ModelProviderAppService;
import com.silver.ai.domain.provider.model.ModelProvider;
import com.silver.ai.interfaces.dto.ProviderRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderAppService providerAppService;

    @GetMapping("/types")
    public Mono<ApiResponse<List<Map<String, Object>>>> getTypes() {
        return Mono.just(ApiResponse.ok(providerAppService.getProviderTypes()));
    }

    @GetMapping
    public Mono<ApiResponse<List<ModelProvider>>> list() {
        return providerAppService.listProviders().collectList().map(ApiResponse::ok);
    }

    @PostMapping
    public Mono<ApiResponse<ModelProvider>> create(@Valid @RequestBody ProviderRequest req) {
        return providerAppService.createProvider(
                req.getName(), req.getProviderType(), req.getApiKey(),
                req.getBaseUrl(), req.getDefaultModel(),
                req.getEmbeddingModel(), req.getEmbeddingDimensions()
        ).map(ApiResponse::ok);
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<ModelProvider>> update(@PathVariable Long id, @RequestBody ProviderRequest req) {
        return providerAppService.updateProvider(id,
                req.getName(), req.getApiKey(), req.getBaseUrl(),
                req.getDefaultModel(), req.getEmbeddingModel(), req.getEmbeddingDimensions()
        ).map(ApiResponse::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        return providerAppService.deleteProvider(id).then(Mono.fromCallable(ApiResponse::ok));
    }

    @PutMapping("/{id}/toggle")
    public Mono<ApiResponse<Void>> toggle(@PathVariable Long id) {
        return providerAppService.toggleProvider(id).then(Mono.fromCallable(ApiResponse::ok));
    }

    @PostMapping("/{id}/validate")
    public Mono<ApiResponse<String>> validateConnection(@PathVariable Long id) {
        return providerAppService.validateConnection(id).map(ApiResponse::ok);
    }
}
