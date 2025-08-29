package com.silver;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@Data
@SpringBootTest
public class MCPTest {

    private ChatClient ollamaChatClient;

    private ChatClient OpenAiChatClient;

    private ChatClient zhiPuAiChatClient;


    @Autowired
    public void setOllamaChatClient(OllamaChatModel ollamaChatModel, OpenAiChatModel openAiChatModel,ZhiPuAiChatModel zhiPuAiChatModel, ToolCallbackProvider toolCallbackProvider, ChatMemory chatMemory) {
        this.ollamaChatClient = ChatClient.builder(ollamaChatModel).defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        ).defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()).build();

        this.OpenAiChatClient = ChatClient.builder(openAiChatModel).defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        ).defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()).build();

        this.zhiPuAiChatClient = ChatClient.builder(zhiPuAiChatModel).defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        ).defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()).build();
    }

    @Test
    public void test_tool() {
        String userInput = "有哪些工具可以使用，列出所有可使用的工具名称";
        var chatClient = zhiPuAiChatClient
                .prompt().options(OllamaOptions.builder()
                        .model("glm-z1-air")
                        .build());

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.user(userInput).call().content());
    }

    @Test
    public void test_workflow() {
        String userInput = "获取电脑配置,在 C:\\Users\\86132\\Documents\\mcp 创建 电脑.txt 电脑配置写入 电脑.txt";
        var chatClient = ollamaChatClient
                .prompt().options(OllamaOptions.builder()
                        .model("qwen3")
                        .build());
        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.user(userInput).call().chatResponse());

    }


    @Test
    public void test_csdn() {
        String userInput = """
                    我需要你帮我生成一篇文章，要求如下；
                                    
                    1. 场景为互联网大厂java求职者面试
                    2. 提问的场景方案可包括但不限于；音视频场景,内容社区与UGC,AIGC,游戏与虚拟互动,电商场景,本地生活服务,共享经济,支付与金融服务,互联网医疗,健康管理,医疗供应链,企业协同与SaaS,产业互联网,大数据与AI服务,在线教育,求职招聘,智慧物流,供应链金融,智慧城市,公共服务数字化,物联网应用,Web3.0与区块链,安全与风控,广告与营销,能源与环保。                
                    3. 按照故事场景，以严肃的面试官和搞笑的程序员小范进行提问，小范对简单问题可以回答出来，回答好了面试官还会夸赞和引导。复杂问题含糊其辞，回答的不清晰。
                    4. 每次进行3轮提问，每轮可以有3-5个问题。这些问题要有技术业务场景上的衔接性，循序渐进引导提问。最后是面试官让程序员回家等通知类似的话术。
                    5. 提问后把问题的答案详细的，写到文章最后，讲述出业务场景和技术点，让小白可以学习下来。
                                    
                    根据以上内容，不要阐述其他信息，请直接提供；文章标题（需要含带技术点）、文章内容、文章标签（多个用英文逗号隔开）、文章简述（100字）
                                    
                    将以上内容 发布文章到CSDN
                    """;
        var chatClient = zhiPuAiChatClient
                .prompt(userInput).options(OllamaOptions.builder()
                        .model("glm-4-air-250414")
                        .build());
        log.info("执行结果:{} {}", userInput, chatClient.call().content());
    }

}