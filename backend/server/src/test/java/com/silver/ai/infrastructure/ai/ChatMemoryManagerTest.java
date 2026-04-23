package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.MessageRole;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class ChatMemoryManagerTest {

    private final ChatMemoryManager manager = new ChatMemoryManager(
            new ChatOrchestratorConfig(), mock(PromptTemplateEngine.class));

    @Test
    void buildMessagesShouldAddSystemPromptAndMapRoles() {
        Conversation conversation = Conversation.builder().id(1L).build();
        conversation.addMessage(MessageRole.USER, "hi");
        conversation.addMessage(MessageRole.ASSISTANT, "hello");

        List<Message> messages = manager.buildMessages(conversation, "system", 10);

        assertEquals(3, messages.size());
        assertInstanceOf(SystemMessage.class, messages.get(0));
        assertInstanceOf(UserMessage.class, messages.get(1));
        assertInstanceOf(AssistantMessage.class, messages.get(2));
    }

    @Test
    void buildMessagesShouldTrimHistoryWhenContextExceedsLimit() {
        String longText = "x".repeat(5000);
        Conversation conversation = Conversation.builder().id(1L).build();
        conversation.addMessage(MessageRole.USER, longText);
        conversation.addMessage(MessageRole.ASSISTANT, longText);
        conversation.addMessage(MessageRole.USER, longText);

        List<Message> messages = manager.buildMessages(conversation, null, 10);

        assertEquals(2, messages.size());
        assertInstanceOf(AssistantMessage.class, messages.get(0));
        assertInstanceOf(UserMessage.class, messages.get(1));
    }
}