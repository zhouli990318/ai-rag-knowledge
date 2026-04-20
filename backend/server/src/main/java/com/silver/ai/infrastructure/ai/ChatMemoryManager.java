package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatMessage;
import com.silver.ai.domain.chat.model.Conversation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
@SuppressWarnings("null")
public class ChatMemoryManager {

    private static final int MAX_CONTEXT_CHARS = 12000;

    public List<Message> buildMessages(Conversation conversation, String systemPrompt, int windowSize) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        List<ChatMessage> history = conversation.getContextMessages(windowSize);
        Deque<ChatMessage> retained = new ArrayDeque<>();
        int totalChars = 0;

        for (int index = history.size() - 1; index >= 0; index--) {
            ChatMessage message = history.get(index);
            int messageChars = estimateMessageChars(message);
            if (totalChars + messageChars > MAX_CONTEXT_CHARS && !retained.isEmpty()) {
                break;
            }
            retained.addFirst(message);
            totalChars += messageChars;
        }

        for (ChatMessage message : retained) {
            switch (message.getRole()) {
                case USER -> messages.add(new UserMessage(message.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(message.getContent()));
                case SYSTEM -> messages.add(new SystemMessage(message.getContent()));
            }
        }

        return messages;
    }

    private int estimateMessageChars(ChatMessage message) {
        return message.getContent() == null ? 0 : message.getContent().length() + 32;
    }
}