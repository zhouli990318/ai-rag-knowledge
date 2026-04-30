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
            """),

    SUGGEST_FOLLOW_UP("""
            你是追问建议专家。基于以下用户与助手的对话，生成 3 个用户最可能继续追问的简短问题。

            核心要求：
            1. 从助手回答中**提及但未展开的概念、术语、限制条件、对比项**中挖掘追问点
            2. 每个问题 8–25 字，必须以"？"结尾
            3. 三个问题分别覆盖不同维度：深入原理、实践应用、对比或局限
            4. 禁止与助手已回答的内容重复，禁止空泛追问（如"能详细说说吗""还有其他的吗"）
            5. 输出格式：每行一个问题，纯文本，不加编号/前缀/引号/解释

            对话：
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