package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.SystemSettingsAppService;
import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsAppService settingsAppService;

    @GetMapping
    public Mono<ApiResponse<ChatOrchestratorConfig>> getSettings() {
        return settingsAppService.getSettings().map(ApiResponse::ok);
    }

    @PutMapping
    public Mono<ApiResponse<ChatOrchestratorConfig>> updateSettings(@RequestBody ChatOrchestratorConfig config) {
        return settingsAppService.updateSettings(config).map(ApiResponse::ok);
    }
}
