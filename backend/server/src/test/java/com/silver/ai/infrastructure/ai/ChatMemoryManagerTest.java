package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.ChatOrchestratorConfig;
import com.silver.ai.domain.chat.model.Conversation;
import com.silver.ai.domain.chat.model.DomainMessage;
import com.silver.ai.domain.chat.model.MessageRole;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ChatMemoryManagerTest {

    private final ChatMemoryManager manager = new ChatMemoryManager(
            new ChatOrchestratorConfig(), mock(PromptRendererPort.class));

    @Test
    void buildMessagesShouldAddSystemPromptAndMapRoles() {
        Conversation conversation = Conversation.builder().id(1L).build();
        conversation.addMessage(MessageRole.USER, "hi");
        conversation.addMessage(MessageRole.ASSISTANT, "hello");

        List<DomainMessage> messages = manager.buildMessages(conversation, "system", 10);

        assertEquals(3, messages.size());
        assertEquals(MessageRole.SYSTEM, messages.get(0).role());
        assertEquals(MessageRole.USER, messages.get(1).role());
        assertEquals(MessageRole.ASSISTANT, messages.get(2).role());
    }

    @Test
    void buildMessagesShouldTrimHistoryWhenContextExceedsLimit() {
        String longText = "x".repeat(5000);
        Conversation conversation = Conversation.builder().id(1L).build();
        conversation.addMessage(MessageRole.USER, longText);
        conversation.addMessage(MessageRole.ASSISTANT, longText);
        conversation.addMessage(MessageRole.USER, longText);

        List<DomainMessage> messages = manager.buildMessages(conversation, null, 10);

        assertEquals(2, messages.size());
        assertEquals(MessageRole.ASSISTANT, messages.get(0).role());
        assertEquals(MessageRole.USER, messages.get(1).role());
    }
}