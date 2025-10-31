package com.silver.mcpserver.infrastructure.adapter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.silver.mcpserver.domain.adapter.INoticePort;
import com.silver.mcpserver.domain.model.request.NoticeFunctionRequest;
import com.silver.mcpserver.domain.model.response.NoticeFunctionResponse;
import com.silver.mcpserver.infrastructure.gateway.IWeChatService;
import com.silver.mcpserver.infrastructure.gateway.dto.WeChatTemplateMessageDTO;
import com.silver.mcpserver.infrastructure.gateway.dto.WeChatTokenResponseDTO;
import com.silver.mcpserver.types.properties.WeChatApiProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WeChatNoticePort implements INoticePort {

    @Autowired
    private WeChatApiProperties weChatApiProperties;

    @Autowired
    private IWeChatService weChatService;

    private Cache<String, WeChatTokenResponseDTO> weChatTokenCache = CacheBuilder.newBuilder().maximumSize(1).build();

    @Override
    public NoticeFunctionResponse noticeMessage(NoticeFunctionRequest request) throws IOException {

        WeChatTokenResponseDTO weChatToken = weChatTokenCache.getIfPresent(weChatApiProperties.getAppId());
        if (weChatToken == null || weChatToken.isExpired()) {
            Call<WeChatTokenResponseDTO> call = weChatService.getAccessToken("client_credential", weChatApiProperties.getAppId(), weChatApiProperties.getAppSecret());
            weChatToken = call.execute().body();
            if (weChatToken == null) {
                throw new IOException("Failed to retrieve WeChat access token");
            }
            weChatTokenCache.put(weChatApiProperties.getAppId(), weChatToken);
        }

        WeChatTemplateMessageDTO templateMessageDTO = getWeChatTemplateMessageDTO(request);

        Call<Void> call = weChatService.sendMessage(weChatToken.getToken(), templateMessageDTO);
        call.execute();

        NoticeFunctionResponse weChatNoticeFunctionResponse = new NoticeFunctionResponse();
        weChatNoticeFunctionResponse.setSuccess(true);

        return weChatNoticeFunctionResponse;

    }

    private @NotNull WeChatTemplateMessageDTO getWeChatTemplateMessageDTO(NoticeFunctionRequest request) {
        Map<String, Map<String, String>> data = Map.of(
                WeChatTemplateMessageDTO.TemplateKey.platform.getCode(), Map.of("value", request.getPlatform()),
                WeChatTemplateMessageDTO.TemplateKey.subject.getCode(), Map.of("value", request.getSubject()),
                WeChatTemplateMessageDTO.TemplateKey.description.getCode(), Map.of("value", request.getDescription())
        );

        WeChatTemplateMessageDTO templateMessageDTO = new WeChatTemplateMessageDTO(weChatApiProperties.getToUser(), weChatApiProperties.getTemplateId());
        templateMessageDTO.setUrl(request.getJumpUrl());
        templateMessageDTO.setData(data);
        return templateMessageDTO;
    }
}
