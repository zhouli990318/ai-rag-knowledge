package com.silver.ai.infrastructure.ai;

import com.silver.ai.domain.chat.model.PromptTemplates;
import com.silver.ai.domain.knowledge.port.PromptRendererPort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateEngine implements PromptRendererPort {

    @Override
    public String render(PromptTemplates template) {
        return render(template, Map.of());
    }

    @Override
    public String render(PromptTemplates template, Map<String, ?> variables) {
        String rendered = template.getTemplate();
        for (var entry : variables.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}