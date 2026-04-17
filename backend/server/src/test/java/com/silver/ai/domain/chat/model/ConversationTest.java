package com.silver.ai.domain.chat.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    @Test
    void addMessageShouldAppendMessageAndInitializeTitleForFirstUserMessage() {
        Conversation conversation = Conversation.builder().id(10L).build();

        conversation.addMessage(MessageRole.USER, "hello world");

        assertEquals(1, conversation.getMessages().size());
        assertEquals(10L, conversation.getMessages().getFirst().getConversationId());
        assertEquals("hello world", conversation.getTitle());
        assertEquals(MessageRole.USER, conversation.getMessages().getFirst().getRole());
    }

    @Test
    void addMessageShouldTruncateLongTitle() {
        Conversation conversation = Conversation.builder().build();

        conversation.addMessage(MessageRole.USER, "123456789012345678901234567890123456789012345678901234567890");

        assertEquals("12345678901234567890123456789012345678901234567890...", conversation.getTitle());
    }

    @Test
    void getContextMessagesShouldReturnLatestWindowAndSupportRagSwitching() {
        Conversation conversation = Conversation.builder().id(1L).build();
        conversation.addMessage(MessageRole.USER, "one");
        conversation.addMessage(MessageRole.ASSISTANT, "two");
        conversation.addMessage(MessageRole.USER, "three");

        List<ChatMessage> context = conversation.getContextMessages(2);
        conversation.enableRag(9L);
        conversation.updateModel(7L, "gpt-test");
        conversation.disableRag();

        assertEquals(2, context.size());
        assertEquals("two", context.get(0).getContent());
        assertEquals("three", context.get(1).getContent());
        assertFalse(conversation.isRagEnabled());
        assertEquals(7L, conversation.getProviderId());
        assertEquals("gpt-test", conversation.getModel());
    }
}