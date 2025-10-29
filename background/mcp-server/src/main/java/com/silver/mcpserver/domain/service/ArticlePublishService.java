package com.silver.mcpserver.domain.service;


import com.silver.mcpserver.domain.adapter.IArticlePublisherPort;
import com.silver.mcpserver.domain.model.request.ArticleFunctionRequest;
import com.silver.mcpserver.domain.model.response.ArticleFunctionResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class ArticlePublishService {

    @Resource
    private IArticlePublisherPort port;

    @Tool(description = "发布文章到CSDN")
    public ArticleFunctionResponse saveArticle(@ToolParam(description = "提供；文章标题、文章内容、文章标签、文章简述") ArticleFunctionRequest request) throws IOException {
        log.info("CSDN发帖，标题:{} 内容:{} 标签:{}", request.getTitle(), request.getMarkdownContent(), request.getTags());
        return port.writeArticle(request);
    }
}
