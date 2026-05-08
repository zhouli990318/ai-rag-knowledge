package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.TraceAppService;
import com.silver.ai.domain.chat.model.ChatTraceContext;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 链路追踪查询 API
 */
@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceAppService traceAppService;

    @GetMapping("/{traceId}")
    public Mono<ApiResponse<ChatTraceContext>> getTrace(@PathVariable String traceId) {
        return traceAppService.getTrace(traceId)
                .map(ApiResponse::ok);
    }

    @GetMapping("/conversation/{conversationId}")
    public Mono<ApiResponse<List<ChatTraceContext>>> getTracesByConversation(
            @PathVariable Long conversationId) {
        return traceAppService.getTracesByConversation(conversationId)
                .collectList()
                .map(ApiResponse::ok);
    }
}
