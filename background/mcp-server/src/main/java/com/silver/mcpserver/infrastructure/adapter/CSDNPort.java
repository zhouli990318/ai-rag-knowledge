package com.silver.mcpserver.infrastructure.adapter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.silver.mcpserver.domain.adapter.IArticlePublisherPort;
import com.silver.mcpserver.domain.model.request.ArticleFunctionRequest;
import com.silver.mcpserver.domain.model.response.ArticleFunctionResponse;
import com.silver.mcpserver.infrastructure.gateway.ICSDNService;
import com.silver.mcpserver.infrastructure.gateway.dto.ArticleRequestDTO;
import com.silver.mcpserver.infrastructure.gateway.dto.ArticleResponseDTO;
import com.silver.mcpserver.types.properties.CSDNApiProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
@Component
public class CSDNPort implements IArticlePublisherPort {

    @Resource
    private ICSDNService csdnService;

    @Resource
    private CSDNApiProperties csdnApiProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public ArticleFunctionResponse writeArticle(ArticleFunctionRequest request) throws IOException {

        ArticleRequestDTO articleRequestDTO = new ArticleRequestDTO();
        articleRequestDTO.setTitle(request.getTitle());
        articleRequestDTO.setMarkdowncontent(request.getMarkdownContent());
        articleRequestDTO.setContent(request.getContent());
        articleRequestDTO.setTags(request.getTags());
        articleRequestDTO.setDescription(request.getDescription());
        articleRequestDTO.setCategories(csdnApiProperties.getCategories());

        Call<ArticleResponseDTO> call = csdnService.saveArticle(articleRequestDTO, csdnApiProperties.getCookie());
        Response<ArticleResponseDTO> response = call.execute();
        log.info("请求CSDN发帖 \nreq:{} \nres:{}", objectMapper.writeValueAsString(articleRequestDTO), objectMapper.writeValueAsString(response));

        if (response.isSuccessful()) {
            ArticleResponseDTO articleResponseDTO = response.body();
            if (null == articleResponseDTO) return null;
            ArticleResponseDTO.ArticleData articleData = articleResponseDTO.getData();

            ArticleFunctionResponse articleFunctionResponse = new ArticleFunctionResponse();
            articleFunctionResponse.setCode(articleResponseDTO.getCode());
            articleFunctionResponse.setMsg(articleResponseDTO.getMsg());
            articleFunctionResponse.setArticleData(ArticleFunctionResponse.ArticleData.builder()
                    .url(articleData.getUrl())
                    .id(articleData.getId())
                    .qrcode(articleData.getQrcode())
                    .title(articleData.getTitle())
                    .description(articleData.getDescription())
                    .build());

            return articleFunctionResponse;
        }

        return null;
    }

}
