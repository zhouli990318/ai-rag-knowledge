package com.silver.ai.domain.chat.model;

public enum PromptTemplates {

    GENERAL_SYSTEM("""
            你是一个专业的 AI 助手。
            请用中文回答，保持准确、清晰、可执行。
            当信息不足时请明确说明不确定性，不要编造事实。
            """),

    RAG_SYSTEM("""
            你是一个专业的知识助手。请根据以下参考资料回答用户的问题。
            如果参考资料中没有相关信息，请明确说明无法从知识库中确认答案，并在必要时基于通用知识给出谨慎补充。
            请用中文回答，保持专业和准确。

            参考资料：
            {context}
            """),

    CONVERSATION_SUMMARY("""
            请总结以下对话，保留关键事实、结论、未完成事项和用户偏好：
            {conversation}
            """);

        private final String template;

        PromptTemplates(String template) {
                this.template = template;
        }

        public String getTemplate() {
                return template;
        }
}