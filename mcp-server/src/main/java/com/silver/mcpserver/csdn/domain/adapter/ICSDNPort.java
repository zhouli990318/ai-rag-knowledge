package com.silver.mcpserver.csdn.domain.adapter;


import com.silver.mcpserver.csdn.domain.model.ArticleFunctionRequest;
import com.silver.mcpserver.csdn.domain.model.ArticleFunctionResponse;

import java.io.IOException;

public interface ICSDNPort {

    ArticleFunctionResponse writeArticle(ArticleFunctionRequest request) throws IOException;

}
