package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.PromptTemplates;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateEngineTest {

    private final PromptTemplateEngine engine = new PromptTemplateEngine();

    @Test
    void renderWithoutVariablesShouldReturnTemplateText() {
        String rendered = engine.render(PromptTemplates.GENERAL_SYSTEM);

        assertTrue(rendered.contains("你是一个专业的 AI 助手"));
    }

    @Test
    void renderWithVariablesShouldReplacePlaceholders() {
        String rendered = engine.render(PromptTemplates.RAG_SYSTEM, Map.of("context", "知识片段"));

        assertTrue(rendered.contains("知识片段"));
        assertEquals(-1, rendered.indexOf("{context}"));
    }
}