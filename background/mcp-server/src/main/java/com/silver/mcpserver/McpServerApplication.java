package com.silver.mcpserver;

import com.silver.mcpserver.api.ComputerApi;
import com.silver.mcpserver.domain.service.ArticlePublishService;
import com.silver.mcpserver.domain.service.NoticeService;
import com.silver.mcpserver.infrastructure.gateway.ICSDNService;
import com.silver.mcpserver.infrastructure.gateway.IWeChatService;
import com.silver.mcpserver.types.properties.CSDNApiProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@SpringBootApplication
public class McpServerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider computerTools(ComputerApi computerApi,
                                              ArticlePublishService articlePublishService,
                                              NoticeService noticeService) {
        return MethodToolCallbackProvider.builder().toolObjects(computerApi, articlePublishService, noticeService).build();
    }

    @Bean
    public ICSDNService csdnService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://bizapi.csdn.net/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(ICSDNService.class);
    }

    @Bean
    public IWeChatService weChatService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weixin.qq.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(IWeChatService.class);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("mcp server computer success!");
    }
}
