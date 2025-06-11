package com.silver.controller;


import com.silver.application.AIChatApplication;
import com.silver.domain.ChatMessage;
import org.springframework.http.ResponseEntity;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/v1/{llm}/")
public class AIChatController {

    @Resource
    private AIChatApplication aiChatApplication;


    @GetMapping("generate")
    public ChatResponse generate(@PathVariable("llm") String llm,
                                 @RequestParam("model") String model,
                                 @RequestParam("message") String message) {
        return aiChatApplication.generate(llm, model, message);
    }

    @GetMapping(value = "generate_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(@PathVariable("llm") String llm,
                                             @RequestParam("model") String model,
                                             @RequestParam("message") String message) {
        return aiChatApplication.generateStream(llm, model, message);
    }

    @GetMapping(value = "generate_stream_rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStreamRag(@PathVariable("llm") String llm,
                                                @RequestParam("model") String model,
                                                @RequestParam("ragTag") String ragTag,
                                                @RequestParam("message") String message) {
        return aiChatApplication.generateStreamRag(llm, model, ragTag, message);
    }

    @PostMapping("/save_chat")
    public ResponseEntity<String> saveChat(@RequestParam("userId") String userId,
                                           @RequestParam("message") String message) {
        aiChatApplication.saveChatMessage(userId, message);
        return ResponseEntity.ok("Chat message saved successfully");
    }

    @GetMapping("/chat_history")
    public List<ChatMessage> getChatHistory(@RequestParam("userId") String userId) {
        return aiChatApplication.getChatHistory(userId);
    }

}
