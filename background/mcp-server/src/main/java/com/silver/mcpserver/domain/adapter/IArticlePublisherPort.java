package com.silver.mcpserver.domain.adapter;


import com.silver.mcpserver.domain.model.request.ArticleFunctionRequest;
import com.silver.mcpserver.domain.model.response.ArticleFunctionResponse;

import java.io.IOException;

public interface IArticlePublisherPort {

    ArticleFunctionResponse writeArticle(ArticleFunctionRequest request) throws IOException;

}
