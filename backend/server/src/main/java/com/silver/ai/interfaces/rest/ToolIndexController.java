package com.silver.ai.interfaces.rest;

import com.silver.ai.domain.chat.service.ToolIndexDomainService;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolIndexController {

    private final ToolIndexDomainService toolIndexDomainService;

    /**
     * 手动触发 MCP 工具索引重建
     */
    @PostMapping("/reindex")
    public Mono<ApiResponse<String>> reindex() {
        return Mono.fromRunnable(toolIndexDomainService::reindexAll)
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(ApiResponse.ok("Tool index rebuild triggered"));
    }

    /**
     * 应用启动后自动初始化工具索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Schedulers.boundedElastic().schedule(() -> {
            log.info("Application ready — initializing MCP tool index...");
            toolIndexDomainService.reindexAll();
        });
    }
}
