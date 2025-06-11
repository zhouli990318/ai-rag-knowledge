package com.silver.application;

import com.silver.domain.ChatMessage;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import com.silver.infrastructure.repository.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AIChatApplication {

    private final ChatClient ollamaChatClient;

    private final ChatClient openAIChatClient;

    private final PgVectorStore pgVectorStore;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public AIChatApplication(OllamaChatModel ollamaChatClient, OpenAiChatModel openAIChatClient, PgVectorStore pgVectorStore) {
        this.ollamaChatClient = ChatClient.builder(ollamaChatClient).build();
        this.openAIChatClient = ChatClient.builder(openAIChatClient).build();
        this.pgVectorStore = pgVectorStore;
    }

    public final String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

    public void saveChatMessage(String userId, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setMessage(message);
        chatMessage.setTimestamp(LocalDateTime.now().toString());
        chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String userId) {
        return chatMessageRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public ChatResponse generate(String llm, String model, String message) {
        if ("ollama".equals(llm)) {
            return ollamaChatClient.prompt().user(message).options(ChatOptions.builder().model(model).build()).call().chatResponse();
        } else if ("openai".equals(llm)) {
            return openAIChatClient.prompt().user(message).options(ChatOptions.builder().model(model).build()).call().chatResponse();
        } else {
            return null;
        }
    }

    public Flux<ChatResponse> generateStream(String llm, String model, String message) {
        return Map.of(
                "ollama", ollamaChatClient.prompt().user(message).options(ChatOptions.builder().model(model).build()).stream().chatResponse(),
                "openai", openAIChatClient.prompt().user(message).options(ChatOptions.builder().model(model).build()).stream().chatResponse()
        ).getOrDefault(llm, Flux.empty());
    }

    public Flux<ChatResponse> generateStreamRag(String llm, String model, String ragTag, String message) {
        SearchRequest request = SearchRequest.builder().query(message).topK(5)
                .filterExpression("knowledge == '" + ragTag + "'")
                .build();
        List<Document> documents = pgVectorStore.similaritySearch(request);
        if (documents == null) {
            documents = new ArrayList<>();
        }
        String documentCollectors = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of("documents", documentCollectors));

        List<Message> messages = Arrays.asList(new UserMessage(message), ragMessage);

        return Map.of(
                "ollama", ollamaChatClient.prompt().messages(messages).options(ChatOptions.builder().model(model).build()).stream().chatResponse(),
                "openai", openAIChatClient.prompt().messages(messages).options(ChatOptions.builder().model(model).build()).stream().chatResponse()
        ).getOrDefault(llm, Flux.empty());
    }
}