package com.silver.ai.interfaces.rest;

import com.silver.ai.domain.chat.model.IntentNode;
import com.silver.ai.domain.chat.port.IntentNodeRepository;
import com.silver.ai.shared.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 意图树管理 API
 */
@RestController
@RequestMapping("/api/v1/intent-tree")
@RequiredArgsConstructor
public class IntentTreeController {

    private final IntentNodeRepository intentNodeRepository;

    @GetMapping
    public Mono<ApiResponse<List<IntentNode>>> getTree() {
        return intentNodeRepository.findPublishedRoots()
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<IntentNode>> getNode(@PathVariable Long id) {
        return intentNodeRepository.findById(id)
                .map(ApiResponse::ok);
    }

    @GetMapping("/{id}/children")
    public Mono<ApiResponse<List<IntentNode>>> getChildren(@PathVariable Long id) {
        return intentNodeRepository.findByParentId(id)
                .collectList()
                .map(ApiResponse::ok);
    }

    @PostMapping
    public Mono<ApiResponse<IntentNode>> createNode(@RequestBody IntentNode node) {
        return intentNodeRepository.save(node)
                .map(ApiResponse::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteNode(@PathVariable Long id) {
        return intentNodeRepository.deleteById(id)
                .then(Mono.just(ApiResponse.ok(null)));
    }
}
