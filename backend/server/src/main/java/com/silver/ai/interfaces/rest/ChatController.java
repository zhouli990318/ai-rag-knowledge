package com.silver.ai.interfaces.rest;

import com.silver.ai.application.service.ChatAppService;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.interfaces.dto.ChatRequest;
import com.silver.ai.shared.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatAppService chatAppService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        return chatAppService.streamChat(
                        request.getConversationId(),
                        request.getProviderId(),
                        request.getModel(),
                        request.getMessage(),
                        request.getKnowledgeBaseId(),
                        request.getSystemPrompt(),
                        request.getMcpServerIds()
                )
                .map(text -> ServerSentEvent.<String>builder().data(text).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done").data("[DONE]").build()));
    }

    @PostMapping(value = "/streamable", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> streamableChat(@Valid @RequestBody ChatRequest request) {
        return chatAppService.streamChat(
                        request.getConversationId(),
                        request.getProviderId(),
                        request.getModel(),
                        request.getMessage(),
                        request.getKnowledgeBaseId(),
                        request.getSystemPrompt(),
                        request.getMcpServerIds()
                )
                .map(text -> text + "\n");
    }

    @PostMapping
    public Mono<ApiResponse<String>> chat(@Valid @RequestBody ChatRequest request) {
        return chatAppService.chat(
                request.getConversationId(),
                request.getProviderId(),
                request.getModel(),
                request.getMessage(),
                request.getKnowledgeBaseId(),
                request.getSystemPrompt(),
                request.getMcpServerIds()
        ).map(ApiResponse::ok);
    }

    @GetMapping("/conversations")
    public Mono<ApiResponse<List<Conversation>>> getConversations() {
        return chatAppService.getConversations().collectList().map(ApiResponse::ok);
    }

    @GetMapping("/conversations/{id}")
    public Mono<ApiResponse<Conversation>> getConversation(@PathVariable Long id) {
        return chatAppService.getConversation(id).map(ApiResponse::ok);
    }

    @DeleteMapping("/conversations/{id}")
    public Mono<ApiResponse<Void>> deleteConversation(@PathVariable Long id) {
        return chatAppService.deleteConversation(id).then(Mono.fromCallable(ApiResponse::ok));
    }
}
